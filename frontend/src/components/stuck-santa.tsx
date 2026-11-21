export function StuckSanta({ freed }: { freed: boolean }) {
  return (
    <svg viewBox="0 0 380 310" fill="none" role="img" aria-label={freed ? "굴뚝에서 상반신을 쑥 빼낸 산타" : "배가 굴뚝에 끼어 양손으로 가장자리를 짚은 산타"}>
      <ellipse cx="187" cy="283" rx="125" ry="10" fill="#242424" opacity=".07" />
      <ellipse cx="180" cy="181" rx="133" ry="96" fill="#f5f5f3" />
      <path d="m57 151 12 3m-8-19 10 8m228 18 13-4m-13 19 15 1" stroke="#a8a8a2" strokeWidth="2" strokeLinecap="round" />

      {/* Head, shoulders and arms share one group, including during rescue. */}
      <g className={freed ? "toonsanta-freed" : "toonsanta-stuck"} stroke="#303030" strokeWidth="2.8" strokeLinecap="round" strokeLinejoin="round">
        <path d="M139 144c-18 0-31 14-39 35l-6 19 25 9 14-26m76-37c19 0 33 14 41 35l7 19-25 9-15-26" fill="#d94636" />
        <path d="M143 137c-21 10-27 31-27 64v26h118v-26c0-34-7-54-29-64Z" fill="#df4937" />
        <path d="M173 154v47" stroke="#fff" strokeWidth="11" />
        <path d="M117 202h116v16H117Z" fill="#303030" />
        <rect x="163" y="200" width="24" height="21" rx="3" fill="#dddcd8" />
        <rect x="170" y="206" width="10" height="9" rx="1" fill="#303030" stroke="none" />
        <path d="M133 108c-7 11-5 34 5 46 9 11 23 19 36 22 17-5 31-14 39-29 6-13 7-30-1-41Z" fill="#fff" />
        <ellipse cx="137" cy="106" rx="7" ry="10" fill="#efc1a4" />
        <ellipse cx="211" cy="106" rx="7" ry="10" fill="#efc1a4" />
        <path d="M140 86c12-14 56-13 68 0v28c0 17-15 28-34 28-18 0-34-11-34-28Z" fill="#f3cdb3" />
        <path d="m146 95 11-3m36 3 9 2" strokeWidth="3" />
        <path d="M153 104v5m43-4v5" strokeWidth="3.5" />
        <ellipse cx="148" cy="116" rx="6" ry="4" fill="#e7a28c" stroke="none" />
        <ellipse cx="200" cy="116" rx="6" ry="4" fill="#e7a28c" stroke="none" />
        <path d="M174 126c-8-9-20-5-24 5 8 6 18 4 24-5Zm0 0c8-9 20-5 24 5-8 6-18 4-24-5Z" fill="#fff" strokeWidth="2" />
        <ellipse cx="174" cy="119" rx="10" ry="8" fill="#d95a48" strokeWidth="2" />
        <path d="M169 143q5 3 10 0" strokeWidth="2" />
        <path d="M138 80c4-24 18-46 40-46 21 0 34 15 41 30l-17 7-12-14 12 24Z" fill="#df4937" />
        <circle cx="220" cy="67" r="10" fill="#fff" />
        <path d="M139 78c18-5 50-5 69 0 4 1 6 5 5 9-1 4-5 6-9 5-21-4-42-4-61 0-5 1-9-2-10-6-1-4 2-7 6-8Z" fill="#fff" />
        <path d="m103 185 19 6m107 0 19-6" stroke="#fff" strokeWidth="10" />
        <path d="M105 190c-8-4-15 0-16 7-1 7 4 12 12 12h14c6 0 10-5 8-10-1-4-5-7-9-7l-2-5c-2-4-7-2-7 3Z" fill="#fff" />
        <path d="M244 190c8-4 15 0 16 7 1 7-4 12-12 12h-14c-6 0-10-5-8-10 1-4 5-7 9-7l2-5c2-4 7-2 7 3Z" fill="#fff" />
      </g>

      <path d="M105 218h144v58H105Z" fill="#deded9" stroke="#303030" strokeWidth="2.8" />
      <path d="M106 241h142m-142 22h142m-97-45v23m58-23v23m-86 0v22m58-22v22m-28 0v13m57-13v13" stroke="#81817a" strokeWidth="1.8" />
      <rect x="93" y="209" width="168" height="17" rx="3" fill="#f2f2ef" stroke="#303030" strokeWidth="2.8" />
      <path d="M96 209c12-6 18 2 29-1s17 2 29-1 17 2 29 0 18 3 30-1 18 2 29 0 13 2 16 3" stroke="#fff" strokeWidth="6" strokeLinecap="round" />
      <path d="m304 231 4-9 3 9 9 2-8 5-1 9-6-7-9 1 5-7Z" fill="#fff" stroke="#a8a8a2" strokeWidth="1.5" />
    </svg>
  );
}

