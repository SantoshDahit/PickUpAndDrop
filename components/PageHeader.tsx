export default function PageHeader({
  script,
  title,
  subtitle,
}: {
  script: string;
  title: string;
  subtitle?: string;
}) {
  return (
    <section className="hero-band border-b border-line">
      <div className="mx-auto max-w-6xl px-5 py-12 sm:py-14 text-center">
        <p className="script text-[24px] mb-1">{script}</p>
        <h1 className="text-[2.1rem] sm:text-[2.4rem]">{title}</h1>
        {subtitle && (
          <p className="text-ink-soft mt-3 max-w-xl mx-auto text-[15.5px]">{subtitle}</p>
        )}
      </div>
    </section>
  );
}
