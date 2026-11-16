import Link from "next/link";

export function FlowFrame({ children, title, description, back = "/", step, action, registration = false }: {
  registration?: boolean;
  children: React.ReactNode;
  title: React.ReactNode;
  description?: React.ReactNode;
  back?: string;
  step?: number;
  action?: React.ReactNode;
}) {
  return <div className="flow-app">
    <header className="flow-header"><Link href={back} className="flow-back" aria-label="이전 단계">←</Link><Link href="/" className="flow-brand">산타<span>컷</span><i>✳</i></Link><span className="flow-preview-tag">{registration ? "실제 참가 등록" : "화면 미리보기"}</span></header>
    {step !== undefined && <nav className="flow-stepper" aria-label="진행 단계">{["기록 준비", "참가 정보", "산타 심사"].map((label, index) => <span key={label} aria-current={index === step ? "step" : undefined} className={index <= step ? "is-active" : ""}><b>{index + 1}</b>{label}{index < 2 && <i aria-hidden="true">· · ·</i>}</span>)}</nav>}
    <main className="flow-content"><p className="flow-kicker">SANTA CUT / 2026</p><h1>{title}</h1>{description && <p className="flow-description">{description}</p>}{children}</main>
    <p className="flow-prototype-note">{registration ? "참가 정보는 서버에 저장됩니다. 파일 전송·AI 심사는 아직 예시입니다." : "파일 전송·AI 심사는 실행되지 않는 예시 화면입니다."}</p>
    {action && <div className="flow-action">{action}</div>}
  </div>;
}

export function NextLink({ href, children, secondary = false }: { href: string; children: React.ReactNode; secondary?: boolean }) {
  return <Link href={href} className={secondary ? "flow-secondary" : "flow-primary"}>{children}<span aria-hidden="true">↗</span></Link>;
}

export function InfoNote({ children }: { children: React.ReactNode }) {
  return <div className="flow-info"><span aria-hidden="true">ⓘ</span><p>{children}</p></div>;
}
