export default function Home() {
  return (
    <main className="flex min-h-screen items-center justify-center px-6 py-16">
      <div className="w-full max-w-xl space-y-6">
        <p className="text-sm font-semibold tracking-widest text-red-700">SANTACUT</p>
        <h1 className="text-4xl font-bold tracking-tight sm:text-5xl">너 올해 산타컷 통과 가능?</h1>
        <p className="text-lg leading-8 text-zinc-600">
          AI가 분석하는 나의 산타 점수. 산타컷을 준비하고 있어요.
        </p>
      </div>
    </main>
  );
}
