declare module 'lucide-react' {
  import type { ComponentType, SVGProps } from 'react'
  type Icon = ComponentType<SVGProps<SVGSVGElement> & { size?: number; strokeWidth?: number }>
  export const Activity: Icon
  export const ArrowLeftRight: Icon
  export const ArrowUpRight: Icon
  export const BarChart3: Icon
  export const Check: Icon
  export const ChevronRight: Icon
  export const CircleDollarSign: Icon
  export const Clock3: Icon
  export const Command: Icon
  export const FileText: Icon
  export const LayoutDashboard: Icon
  export const LogOut: Icon
  export const Plus: Icon
  export const ShieldCheck: Icon
  export const Wallet: Icon
}
