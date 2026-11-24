"use client"

import {
  ArrowsOutIcon,
  ChatCircleDotsIcon,
  HandWavingIcon,
  MicrophoneIcon,
  MicrophoneSlashIcon,
  MonitorArrowUpIcon,
  PhoneDisconnectIcon,
  UsersThreeIcon,
  VideoCameraIcon,
  VideoCameraSlashIcon,
} from "@phosphor-icons/react"
import { Avatar, AvatarFallback } from "@workspace/ui/components/avatar"
import { Button } from "@workspace/ui/components/button"
import { useState } from "react"

export default function VideoCall() {
  const [micEnabled, setMicEnabled] = useState(true)
  const [cameraEnabled, setCameraEnabled] = useState(true)
  const [handRaised, setHandRaised] = useState(false)
  const [sharing, setSharing] = useState(false)

  return (
    <section className="flex h-full min-h-0 flex-col gap-4 bg-muted/30 p-4 sm:p-6">
      <div className="grid min-h-0 flex-1 grid-cols-1 gap-3 md:grid-cols-[minmax(0,1fr)_220px]">
        <div className="relative flex min-h-[300px] items-center justify-center overflow-hidden rounded-2xl border bg-zinc-950 p-4 text-white shadow-sm sm:min-h-[380px]">
          <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,var(--tw-gradient-stops))] from-zinc-800 via-zinc-950 to-black" />
          <div className="relative flex flex-col items-center gap-4 text-center">
            <Avatar className="size-24 border border-white/20 sm:size-32">
              <AvatarFallback className="bg-linear-to-br from-amber-400 to-orange-700 text-3xl font-semibold text-white sm:text-4xl">
                YB
              </AvatarFallback>
            </Avatar>
            <div>
              <p className="font-semibold">Youssef B.</p>
              <p className="mt-1 text-sm text-white/60">Speaking</p>
            </div>
          </div>
          <div className="absolute bottom-4 left-4 flex items-center gap-2 rounded-lg bg-black/45 px-3 py-2 text-sm backdrop-blur">
            <span className="size-2 rounded-full bg-emerald-400" />
            Youssef B.
          </div>
          <Button
            variant="ghost"
            size="icon"
            aria-label="Expand video"
            className="absolute top-3 right-3 text-white hover:bg-white/15 hover:text-white"
          >
            <ArrowsOutIcon className="size-4" />
          </Button>
          {handRaised && (
            <span className="absolute top-4 left-4 rounded-full bg-amber-400 px-3 py-1 text-xs font-medium text-black">
              You raised your hand
            </span>
          )}
        </div>

        <div className="grid grid-cols-1 gap-3">
          <div className="relative flex min-h-[130px] items-center justify-center overflow-hidden rounded-2xl border bg-linear-to-br from-sky-500/35 to-indigo-950 text-white">
            <div className="flex h-full w-full items-center justify-center bg-[radial-gradient(ellipse_at_center,var(--tw-gradient-stops))] from-sky-900/50 to-indigo-950">
              <Avatar className="size-16 border border-white/20">
                <AvatarFallback className="bg-sky-700 text-xl font-semibold text-white">
                  AM
                </AvatarFallback>
              </Avatar>
            </div>
            <span className="absolute bottom-2 left-2 rounded-md bg-black/45 px-2 py-1 text-xs backdrop-blur">
              Alex Morgan (you)
            </span>
            {!cameraEnabled && (
              <span
                className="absolute top-2 right-2 rounded-full bg-black/50 p-1.5"
                aria-label="Camera off"
              >
                <VideoCameraSlashIcon className="size-3.5" />
              </span>
            )}
          </div>
        </div>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border bg-background px-3 py-3 sm:px-4">
        <div className="hidden items-center gap-2 text-sm text-muted-foreground sm:flex">
          <UsersThreeIcon className="size-4" />
          <span>2 participants</span>
        </div>
        <div className="mx-auto flex items-center gap-2">
          <Button
            variant={micEnabled ? "outline" : "destructive"}
            size="icon"
            aria-label={micEnabled ? "Mute microphone" : "Turn on microphone"}
            aria-pressed={!micEnabled}
            onClick={() => setMicEnabled((enabled) => !enabled)}
          >
            {micEnabled ? <MicrophoneIcon /> : <MicrophoneSlashIcon />}
          </Button>
          <Button
            variant={cameraEnabled ? "outline" : "destructive"}
            size="icon"
            aria-label={cameraEnabled ? "Turn off camera" : "Turn on camera"}
            aria-pressed={!cameraEnabled}
            onClick={() => setCameraEnabled((enabled) => !enabled)}
          >
            {cameraEnabled ? <VideoCameraIcon /> : <VideoCameraSlashIcon />}
          </Button>
          <Button
            variant={handRaised ? "secondary" : "outline"}
            size="icon"
            aria-label={handRaised ? "Lower hand" : "Raise hand"}
            aria-pressed={handRaised}
            onClick={() => setHandRaised((raised) => !raised)}
          >
            <HandWavingIcon />
          </Button>
          <Button
            variant={sharing ? "secondary" : "outline"}
            size="icon"
            aria-label={sharing ? "Stop sharing screen" : "Share screen"}
            aria-pressed={sharing}
            onClick={() => setSharing((active) => !active)}
          >
            <MonitorArrowUpIcon />
          </Button>
          <Button variant="outline" size="icon" aria-label="Open chat">
            <ChatCircleDotsIcon />
          </Button>
          <Button variant="destructive" size="icon" aria-label="Leave call">
            <PhoneDisconnectIcon />
          </Button>
        </div>
        <span className="hidden text-xs text-muted-foreground tabular-nums lg:block">
          24:10
        </span>
      </div>
    </section>
  )
}
