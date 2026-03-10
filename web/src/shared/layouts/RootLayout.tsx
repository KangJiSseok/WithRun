export default function RootLayout({children}: {children: React.ReactNode}) {
  return (
    <div className="max-w-[430px] min-w-[375px] mx-auto">{children}</div>
  )
}
