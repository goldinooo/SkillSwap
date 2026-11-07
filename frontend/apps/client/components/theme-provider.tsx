"use client"

import * as React from "react"

type Theme = "light" | "dark" | "system"
type ResolvedTheme = Exclude<Theme, "system">

type ThemeContextValue = {
  theme: Theme
  resolvedTheme: ResolvedTheme
  setTheme: React.Dispatch<React.SetStateAction<Theme>>
}

const ThemeContext = React.createContext<ThemeContextValue | null>(null)

function getTheme(value: string | null): Theme {
  return value === "light" || value === "dark" ? value : "system"
}

function ThemeProvider({ children }: Readonly<{ children: React.ReactNode }>) {
  const [theme, setThemeState] = React.useState<Theme>("system")
  const [resolvedTheme, setResolvedTheme] = React.useState<ResolvedTheme>("light")
  const themeRef = React.useRef<Theme>("system")

  const applyTheme = React.useCallback((nextTheme: Theme, systemIsDark?: boolean) => {
    const prefersDark =
      systemIsDark ?? window.matchMedia("(prefers-color-scheme: dark)").matches
    const resolved =
      nextTheme === "system" ? (prefersDark ? "dark" : "light") : nextTheme
    const root = document.documentElement

    root.classList.remove("light", "dark")
    root.classList.add(resolved)
    root.style.colorScheme = resolved
    setResolvedTheme(resolved)
  }, [])

  const setTheme = React.useCallback<
    React.Dispatch<React.SetStateAction<Theme>>
  >(
    (nextTheme) => {
      const value =
        typeof nextTheme === "function" ? nextTheme(themeRef.current) : nextTheme
      themeRef.current = value
      setThemeState(value)
      try {
        window.localStorage.setItem("theme", value)
      } catch {
        // The theme still applies when storage is unavailable.
      }
      applyTheme(value)
    },
    [applyTheme]
  )

  React.useEffect(() => {
    const media = window.matchMedia("(prefers-color-scheme: dark)")
    const initialTheme = getTheme(window.localStorage.getItem("theme"))
    themeRef.current = initialTheme
    setThemeState(initialTheme)
    applyTheme(initialTheme, media.matches)

    const handleSystemChange = (event: MediaQueryListEvent) => {
      if (themeRef.current === "system") {
        applyTheme("system", event.matches)
      }
    }
    const handleStorage = (event: StorageEvent) => {
      if (event.key !== "theme") return
      const nextTheme = getTheme(event.newValue)
      themeRef.current = nextTheme
      setThemeState(nextTheme)
      applyTheme(nextTheme, media.matches)
    }

    media.addEventListener("change", handleSystemChange)
    window.addEventListener("storage", handleStorage)
    return () => {
      media.removeEventListener("change", handleSystemChange)
      window.removeEventListener("storage", handleStorage)
    }
  }, [applyTheme])

  return (
    <ThemeContext.Provider value={{ theme, resolvedTheme, setTheme }}>
      <ThemeHotkey />
      {children}
    </ThemeContext.Provider>
  )
}

function useTheme() {
  const context = React.useContext(ThemeContext)
  if (!context) throw new Error("useTheme must be used within ThemeProvider")
  return context
}

function isTypingTarget(target: EventTarget | null) {
  if (!(target instanceof HTMLElement)) {
    return false
  }

  return (
    target.isContentEditable ||
    target.tagName === "INPUT" ||
    target.tagName === "TEXTAREA" ||
    target.tagName === "SELECT"
  )
}

function ThemeHotkey() {
  const { resolvedTheme, setTheme } = useTheme()

  React.useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if (event.defaultPrevented || event.repeat) {
        return
      }

      if (event.metaKey || event.ctrlKey || event.altKey) {
        return
      }

      if (event.key.toLowerCase() !== "d") {
        return
      }

      if (isTypingTarget(event.target)) {
        return
      }

      setTheme(resolvedTheme === "dark" ? "light" : "dark")
    }

    window.addEventListener("keydown", onKeyDown)

    return () => {
      window.removeEventListener("keydown", onKeyDown)
    }
  }, [resolvedTheme, setTheme])

  return null
}

export { ThemeProvider }
