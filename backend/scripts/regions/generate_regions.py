#!/usr/bin/env python3
"""전국 시·도·시·군·구 지역 시드 SQL(src/main/resources/regions.sql)을 만든다.

vuski/admdongkor 행정동 경계 GeoJSON을 내려받아 시·군·구(sgg)와 시·도(sido)로 묶고,
면적 중심(신발끈 공식, 구멍 제외, 경도에 cos(위도) 보정)을 대표 좌표로 쓴다.
서울(sido 11)은 기존 비공식 코드(KR-11-xxx)를 참가자 데이터가 참조하므로 건너뛴다.
표준 라이브러리만 쓴다. 34MB GeoJSON은 저장소에 넣지 않는다.

사용법:
    python generate_regions.py                       # 고정 커밋 URL에서 내려받아 regions.sql을 쓴다
    python generate_regions.py --geojson local.json  # 이미 받은 파일을 쓴다(헤더에는 고정 URL을 적는다)
"""
import argparse
import json
import math
import sys
import tempfile
import urllib.error
import urllib.request
from pathlib import Path

PINNED_URL = (
    "https://raw.githubusercontent.com/vuski/admdongkor/"
    "3bc00489b76696db864bdbea4b19985cdfa8b403/ver20261001/HangJeongDong_ver20261001.geojson"
)
FALLBACK_URL = (
    "https://raw.githubusercontent.com/vuski/admdongkor/"
    "master/ver20261001/HangJeongDong_ver20261001.geojson"
)
DATA_VERSION = "2026-10-01"
SKIPPED_SIDO = "11"  # 서울은 기존 행을 그대로 둔다.
CHILDLESS_SIDO = "36"  # 세종은 시·도 행 하나만 두고 그 자체로 고른다.
DECIMALS = 4
# 일반구가 있는 시. GeoJSON의 sggnm은 `수원시장안구`처럼 붙어 있어 `수원시 장안구`로 띄운다.
CITIES_WITH_GU = (
    "수원시", "성남시", "안양시", "부천시", "안산시", "고양시", "용인시",
    "화성시", "청주시", "천안시", "포항시", "창원시", "전주시",
)
DEFAULT_OUT = Path(__file__).resolve().parents[2] / "src" / "main" / "resources" / "regions.sql"

HEADER = """\
-- 전국 시·도와 시·군·구 지역 시드. scripts/regions/generate_regions.py가 만든 파일이므로 손으로 고치지 않는다.
-- 출처: {url}
-- 데이터 기준일: {version} (vuski/admdongkor ver20261001 행정동 경계)
-- 본 데이터는 통계청 통계지리정보서비스(SGIS, https://sgis.kostat.go.kr)에서 공공누리 제1유형으로 개방한 행정동 경계를 가공한 것이며(가공: vuski/admdongkor, https://github.com/vuski/admdongkor), CC BY 4.0으로 배포됩니다.
-- 대표 좌표: 행정동 폴리곤의 면적 중심(신발끈 공식, 구멍 제외, 경도에 cos(위도) 보정)을 시·군·구와 시·도 단위로 면적 가중 평균하고 소수 4자리로 반올림했다.
-- 대표 좌표는 청사 위치가 아니며 섬 지역(옹진군·신안군·울릉군 등)은 바다 위일 수 있다.
-- 코드: KR-{{sido}}, KR-{{sido}}-{{sgg 뒤 3자리}}. 서울 코드만 비공식 번호이며 서울 행은 schema.sql에 있어 여기서 넣지 않는다.
-- 일반구는 시·도 바로 아래 지역으로 두고 이름을 `수원시 장안구`처럼 쓴다. 세종특별자치시는 하위 지역 없이 그 자체로 고른다.
-- parent_code는 앱이 뜰 때 RegionFacade가 코드 접두어로 채운다. INSERT OR IGNORE라 여러 번 실행해도 결과가 같다.
"""


def download(target: Path) -> str:
    """고정 커밋 URL에서 받고, 없으면(404) master URL로 받는다. 실제로 받은 URL을 돌려준다."""
    for url in (PINNED_URL, FALLBACK_URL):
        try:
            print(f"내려받는 중: {url}", file=sys.stderr)
            urllib.request.urlretrieve(url, target)
            return url
        except urllib.error.HTTPError as error:
            if error.code != 404:
                raise
            print(f"404, 다음 URL을 시도한다: {url}", file=sys.stderr)
    raise SystemExit("GeoJSON을 내려받지 못했다.")


def ring_moments(ring):
    """링 하나의 부호 없는 면적과 중심(경도, 위도). 경도는 링 첫 점 위도의 cos로 줄여 면적을 잰다."""
    scale = math.cos(math.radians(ring[0][1]))
    area2 = cx = cy = 0.0
    for (x0, y0), (x1, y1) in zip((p[:2] for p in ring), (p[:2] for p in ring[1:])):
        cross = x0 * y1 - x1 * y0
        area2 += cross
        cx += (x0 + x1) * cross
        cy += (y0 + y1) * cross
    if area2 == 0:
        return 0.0, 0.0, 0.0
    return abs(area2) / 2 * scale, cx / (3 * area2), cy / (3 * area2)


def feature_moments(geometry):
    """폴리곤 바깥 링은 더하고 구멍은 빼서 (면적, 면적×경도, 면적×위도)를 돌려준다."""
    polygons = geometry["coordinates"] if geometry["type"] == "MultiPolygon" else [geometry["coordinates"]]
    total = [0.0, 0.0, 0.0]
    for polygon in polygons:
        for index, ring in enumerate(polygon):
            area, lon, lat = ring_moments(ring)
            sign = 1 if index == 0 else -1
            total[0] += sign * area
            total[1] += sign * area * lon
            total[2] += sign * area * lat
    return total


def display_name(sgg_name: str) -> str:
    for city in CITIES_WITH_GU:
        if sgg_name.startswith(city) and len(sgg_name) > len(city):
            return f"{city} {sgg_name[len(city):]}"
    return sgg_name


def aggregate(features):
    """시·도와 시·군·구별 면적 모멘트를 모은다. 키는 지역 코드, 값은 [이름, 면적, 면적×경도, 면적×위도]."""
    regions = {}
    for feature in features:
        props = feature["properties"]
        if props["sido"] == SKIPPED_SIDO:
            continue
        moments = feature_moments(feature["geometry"])
        keys = [(f"KR-{props['sido']}", props["sidonm"])]
        if props["sido"] != CHILDLESS_SIDO:
            keys.append((f"KR-{props['sido']}-{props['sgg'][2:]}", display_name(props["sggnm"])))
        for code, name in keys:
            entry = regions.setdefault(code, [name, 0.0, 0.0, 0.0])
            for i in range(3):
                entry[i + 1] += moments[i]
    return regions


def sql_literal(text: str) -> str:
    return "'" + text.replace("'", "''") + "'"


def render(regions, url: str) -> str:
    lines = [HEADER.format(url=url, version=DATA_VERSION)]
    for code in sorted(regions):
        name, area, lon_moment, lat_moment = regions[code]
        lat = round(lat_moment / area, DECIMALS)
        lon = round(lon_moment / area, DECIMALS)
        lines.append(
            "INSERT OR IGNORE INTO regions(code,name,latitude,longitude) VALUES "
            f"({sql_literal(code)},{sql_literal(name)},{lat:.{DECIMALS}f},{lon:.{DECIMALS}f});\n"
        )
    return "".join(lines)


def summarize(regions) -> None:
    sidos = sorted(code for code in regions if code.count("-") == 1)
    for sido in sidos:
        children = sum(1 for code in regions if code.startswith(sido + "-"))
        print(f"{sido} {regions[sido][0]} 하위 {children}", file=sys.stderr)
    print(f"시·도 {len(sidos)}개(서울 제외), 전체 {len(regions)}행", file=sys.stderr)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--geojson", type=Path, help="이미 받은 GeoJSON 경로. 없으면 내려받는다.")
    parser.add_argument("--source-url", default=PINNED_URL, help="--geojson을 줄 때 헤더에 적을 출처 URL")
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT)
    args = parser.parse_args()

    if args.geojson:
        path, url = args.geojson, args.source_url
    else:
        path = Path(tempfile.gettempdir()) / "HangJeongDong_ver20261001.geojson"
        url = download(path)
    try:
        with open(path, encoding="utf-8") as source:
            features = json.load(source)["features"]
    finally:
        if not args.geojson:
            path.unlink(missing_ok=True)

    regions = aggregate(features)
    with open(args.out, "w", encoding="utf-8", newline="\n") as out:
        out.write(render(regions, url))
    summarize(regions)
    print(f"썼다: {args.out}", file=sys.stderr)


if __name__ == "__main__":
    main()
