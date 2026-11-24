import {
  CodeIcon,
  Icon,
  PaletteIcon,
  VideoCameraIcon,
} from "@phosphor-icons/react"

type LabNavigationItemType = {
  label: string
  href: string
  icon: Icon
}

const LabNavigationItems: LabNavigationItemType[] = [
  {
    label: "Canvas",
    href: "/lab",
    icon: PaletteIcon,
  },
  {
    label: "Video Call",
    href: "/lab/video",
    icon: VideoCameraIcon,
  },
  {
    label: "Code Editor",
    href: "/lab/code",
    icon: CodeIcon,
  },
]

function currentTab(pathname: string, href: string): boolean {
  if (href === "/lab") {
    return pathname === href
  }

  return pathname === href || pathname.startsWith(`${href}/`)
}

export { type LabNavigationItemType, LabNavigationItems, currentTab }
