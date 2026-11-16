import { PreviewProvider } from "@/components/flow/preview-context";
import "./flow.css";

export default function FlowLayout({ children }: { children: React.ReactNode }) {
  return <PreviewProvider>{children}</PreviewProvider>;
}
