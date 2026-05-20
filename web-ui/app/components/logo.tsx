import type { ComponentPropsWithRef } from "react";

export default function Logo({ alt = "EterUee", ...props }: ComponentPropsWithRef<"img">) {
  return <img {...props} src="/icon.svg" alt={alt} />;
}
