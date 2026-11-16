"use client";

import { useEffect, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { FlowFrame, InfoNote } from "./frame";
import { usePreview } from "./preview-context";
import { checkNickname, getParticipant, getRegions, isValidNickname, normalizeNickname, participantError, participantKey, registerParticipant, type Participant } from "@/lib/participants";

export function RegistrationScreen() {
  const { data, update } = usePreview();
  const router = useRouter();
  const client = useQueryClient();
  const [nickname, setNickname] = useState(data.nickname);
  const [debounced, setDebounced] = useState("");
  const [provinceCode, setProvinceCode] = useState("");
  const [districtCode, setDistrictCode] = useState("");
  const [error, setError] = useState("");
  const [created, setCreated] = useState<Participant | null>(null);
  const submitting = useRef(false);
  const normalized = normalizeNickname(nickname);
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(normalized), 350);
    return () => clearTimeout(timer);
  }, [normalized]);

  const me = useQuery({ queryKey: participantKey, queryFn: ({ signal }) => getParticipant(signal), staleTime: 0, retry: false });
  const provinces = useQuery({ queryKey: ["regions"], queryFn: ({ signal }) => getRegions(undefined, signal), staleTime: 60 * 60_000, enabled: !me.isPending && !me.isError });
  const savedProvince = provinces.data?.find(region => region.code === me.data?.regionCode || me.data?.regionCode.startsWith(`${region.code}-`));
  const province = me.data ? savedProvince : provinces.data?.find(region => region.code === provinceCode);
  const districts = useQuery({ queryKey: ["regions", province?.code], queryFn: ({ signal }) => getRegions(province!.code, signal), enabled: Boolean(province && !province.selectable), staleTime: 60 * 60_000 });
  const selectedRegion = province?.selectable ? province : districts.data?.find(region => region.code === districtCode && region.selectable);
  const availability = useQuery({
    queryKey: ["nickname-availability", debounced], queryFn: ({ signal }) => checkNickname(debounced, signal),
    enabled: me.data === null && !created && isValidNickname(debounced) && debounced === normalized,
    staleTime: 0, retry: false,
  });

  function continueWith(participant: Participant) {
    const root = provinces.data?.find(region => region.code === participant.regionCode || participant.regionCode.startsWith(`${region.code}-`));
    update({ nickname: participant.nickname, province: root?.name ?? participant.regionCode, district: districts.data?.find(region => region.code === participant.regionCode)?.name ?? "" });
    router.push("/review");
  }
  const registration = useMutation({
    mutationFn: async () => {
      // A different tab may have registered while this form was open.
      const existing = await getParticipant();
      if (existing) return existing;
      const available = await checkNickname(normalized);
      if (!available.available) throw new Error("nickname-taken");
      const participant = await registerParticipant({ nickname: normalized, regionCode: selectedRegion!.code });
      setCreated(participant);
      const session = await getParticipant();
      if (!session || session.id !== participant.id) throw new Error("session-not-preserved");
      return session;
    },
    onSuccess: participant => { client.setQueryData(participantKey, participant); continueWith(participant); },
    onError: failure => {
      if (failure instanceof Error && failure.message === "nickname-taken") {
        setError("이미 사용 중인 닉네임이에요. 다른 이름을 골라 주세요.");
        void client.invalidateQueries({ queryKey: ["nickname-availability", debounced] });
      } else setError(participantError(failure));
    },
    retry: false,
  });

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (submitting.current) return;
    if (!isValidNickname(normalized)) { setError("닉네임은 한글·영문·숫자·밑줄로 2~16자 입력해 주세요."); return; }
    if (!selectedRegion) { setError("시·도와 등록할 지역을 선택해 주세요."); return; }
    if (debounced !== normalized || availability.isFetching || !availability.data?.available || availability.isError) { setError("사용 가능한 닉네임인지 확인한 뒤 다시 시도해 주세요."); return; }
    submitting.current = true;
    setError("");
    try { await registration.mutateAsync(); } catch { /* onError displays a safe message. */ }
    finally { submitting.current = false; }
  }

  let nicknameHelp = "2~16자 · 한글, 영문, 숫자, 밑줄";
  if (normalized) {
    if (!isValidNickname(normalized)) nicknameHelp = "한글·영문·숫자·밑줄로 2~16자 입력해 주세요.";
    else if (debounced !== normalized || availability.isFetching) nicknameHelp = "닉네임 중복 확인 중…";
    else if (availability.isError) nicknameHelp = "중복 확인에 실패했어요. 다시 확인해 주세요.";
    else if (availability.data) nicknameHelp = availability.data.available ? "지금 사용할 수 있어요. 등록 전까지 예약되지는 않아요." : "이미 사용 중인 닉네임이에요.";
  }
  const pending = registration.isPending;
  return <FlowFrame registration back="/upload" step={1} title={<>선물 받을 분,<br />성함이?</>} description="실명 말고 닉네임. 산타도 사생활은 지켜.">
    {me.isPending ? <p role="status">참가 정보를 확인하고 있어요…</p> : me.isError ? <div role="alert"><p>{participantError(me.error)}</p><button className="flow-secondary" onClick={() => void me.refetch()}>참가 정보 다시 확인</button></div> : me.data ? <>
      <InfoNote>이미 참가 중이에요. 저장된 정보로 계속할 수 있어요.</InfoNote>
      <dl className="flow-summary"><div><dt>닉네임</dt><dd>{me.data.nickname}</dd></div><div><dt>등록 지역</dt><dd>{savedProvince?.name} {districts.data?.find(region => region.code === me.data?.regionCode)?.name ?? (!savedProvince?.selectable ? me.data.regionCode : "")}</dd></div></dl>
      <p className="flow-fine-print">새로고침해도 세션 쿠키로 참가 정보를 불러옵니다. 닉네임은 등록 후 변경할 수 없어요.</p>
      <div className="flow-action"><button className="flow-primary" disabled={provinces.isPending || Boolean(savedProvince && !savedProvince.selectable && districts.isPending)} onClick={() => continueWith(me.data!)}>이 정보로 계속 <span aria-hidden="true">↗</span></button></div>
    </> : created ? <div role={pending ? "status" : "alert"}><p>{pending ? "참가 등록을 마쳤어요. 세션을 확인하고 있어요…" : "참가 등록은 완료됐지만 세션을 확인하지 못했어요. 다시 등록하지 말고 쿠키 허용 설정을 확인한 뒤 참가 정보를 다시 불러와 주세요."}</p>{!pending && <button disabled={me.isFetching} className="flow-secondary" onClick={() => void me.refetch()}>참가 정보 다시 확인</button>}</div> : <form onSubmit={submit}>
      <fieldset disabled={pending} className="flow-registration-fields">
        <div className="flow-field"><label htmlFor="nickname">닉네임</label><input id="nickname" value={nickname} maxLength={32} placeholder="예: 루돌프인턴" autoComplete="nickname" aria-describedby="nickname-help" aria-invalid={Boolean(normalized && (!isValidNickname(normalized) || (debounced === normalized && availability.data?.available === false)))} onChange={event => { setNickname(event.target.value); setError(""); }} /><p id="nickname-help" aria-live="polite">{nicknameHelp}</p>{availability.isError && <button type="button" className="flow-text-link" onClick={() => void availability.refetch()}>중복 확인 다시 시도</button>}</div>
        <div className="flow-field"><label htmlFor="province">사는 지역</label>{provinces.isPending ? <p role="status">지역을 불러오고 있어요…</p> : provinces.isError ? <div role="alert"><p>지역을 불러오지 못했어요.</p><button type="button" className="flow-text-link" onClick={() => void provinces.refetch()}>지역 다시 불러오기</button></div> : <div className="flow-select-row">
          <select id="province" aria-label="시·도" value={provinceCode} onChange={event => { setProvinceCode(event.target.value); setDistrictCode(""); setError(""); }}><option value="">시·도 선택</option>{provinces.data?.map(region => <option key={region.code} value={region.code}>{region.name}</option>)}</select>
          {province && !province.selectable && <select aria-label="시·군·구" value={districtCode} disabled={districts.isPending || districts.isError} onChange={event => { setDistrictCode(event.target.value); setError(""); }}><option value="">{districts.isPending ? "불러오는 중…" : "시·군·구 선택"}</option>{districts.data?.filter(region => region.selectable).map(region => <option key={region.code} value={region.code}>{region.name}</option>)}</select>}
        </div>}{districts.isError && province && !province.selectable && <div role="alert"><p>하위 지역을 불러오지 못했어요.</p><button type="button" className="flow-text-link" onClick={() => void districts.refetch()}>하위 지역 다시 불러오기</button></div>}<p>상세 주소는 받지 않아요. 하위 지역이 없는 시·도는 바로 선택할 수 있어요.</p></div>
        <InfoNote>닉네임은 결과·랭킹에 공개되고, 지역은 도착일 계산에 사용돼요. 아래 버튼을 누르면 실제 서버에 참가 정보가 등록됩니다. 파일 전송과 분석은 아직 예시예요.</InfoNote>
        {error && <p className="flow-error" role="alert">{error}</p>}
        <div className="flow-action"><button className="flow-primary" type="submit" disabled={pending || provinces.isPending || provinces.isError || !provinces.data?.length}>{pending ? "참가 등록 중…" : "참가 등록하고 다음"}<span aria-hidden="true">↗</span></button></div>
      </fieldset>
    </form>}
  </FlowFrame>;
}
