import { Github, Google } from "@thesvg/react"
import { Button } from "@workspace/ui/components/button"
import { FieldGroup } from "@workspace/ui/components/field"
import React from "react"

export default function OauthGroup() {
  return (
    <FieldGroup className="grid grid-cols-2">
      <Button variant={"secondary"}>
        <Github className="size-4" variant="mono" />
        Github
      </Button>
      <Button variant={"secondary"}>
        <Google className="size-4" variant="mono" />
        Google
      </Button>
    </FieldGroup>
  )
}
