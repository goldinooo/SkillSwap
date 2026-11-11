"use client"

import { useId, useMemo, useState } from "react"
import { useRouter } from "next/navigation"
import {
  Controller,
  useFieldArray,
  useForm,
  useWatch,
  type FieldError as RHFFieldError,
} from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import {
  ChatCircleIcon,
  CodeIcon,
  CurrencyCircleDollarIcon,
  MagnifyingGlassIcon,
  PencilSimpleIcon,
  PlusIcon,
  StarIcon,
  TrashIcon,
  VideoCameraIcon,
  type Icon,
} from "@phosphor-icons/react"
import { Button } from "@workspace/ui/components/button"
import { Card } from "@workspace/ui/components/card"
import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
  FieldLegend,
  FieldSet,
} from "@workspace/ui/components/field"
import { Input } from "@workspace/ui/components/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@workspace/ui/components/select"

import { MOCK_WEEKLY_POINTS, SESSION_TOOL_OPTIONS } from "./create-lesson-data"
import {
  AvailabilityList,
  LessonPreview,
  PublishChecklist,
  SectionHeader,
} from "./lesson-sidebar"
import {
  DESCRIPTION_MAX,
  LESSON_CATEGORIES,
  LESSON_LEVELS,
  MAX_OUTCOMES,
  PUBLISH_STEPS,
  SESSION_TOOLS,
  lessonSchema,
  type LessonFormInput,
  type LessonFormValues,
  type SessionTool,
} from "./lesson-schema"

const TOOL_ICONS: Record<SessionTool, Icon> = {
  video: VideoCameraIcon,
  chat: ChatCircleIcon,
  code: CodeIcon,
  canvas: PencilSimpleIcon,
}

const OUTCOME_PLACEHOLDERS = [
  "e.g. Type functions and objects with confidence",
  "e.g. Use generics in real code",
  "e.g. Model data with interfaces",
  "e.g. Ship a small typed library",
]

const DEFAULT_VALUES: Partial<LessonFormInput> = {
  title: "",
  description: "",
  level: "Beginner",
  outcomes: [{ text: "" }, { text: "" }],
  prerequisites: "",
  tools: ["video", "chat"],
}

function Required() {
  return (
    <span aria-hidden className="text-(--chart-3)">
      *
    </span>
  )
}

export function CreateLessonForm() {
  const router = useRouter()
  const ids = {
    title: useId(),
    category: useId(),
    description: useId(),
    prerequisites: useId(),
  }
  const [draftMessage, setDraftMessage] = useState<string | null>(null)

  const form = useForm<LessonFormInput, unknown, LessonFormValues>({
    resolver: zodResolver(lessonSchema),
    defaultValues: DEFAULT_VALUES,
    mode: "onTouched",
  })
  const {
    control,
    register,
    handleSubmit,
    getValues,
    trigger,
    formState: { errors, isSubmitting },
  } = form

  const outcomes = useFieldArray({ control, name: "outcomes" })
  const values = useWatch({ control }) as LessonFormInput

  const steps = useMemo(
    () =>
      PUBLISH_STEPS.map((step) => ({
        id: step.id,
        label: step.label,
        done: step.schema.safeParse(values).success,
      })),
    [values]
  )

  async function onPublish(data: LessonFormValues) {
    // TODO: replace with the create lesson API call.
    await new Promise((resolve) => setTimeout(resolve, 600))
    console.info("Publish lesson", data)
    router.push("/dashboard/lessons")
  }

  async function onSaveDraft() {
    // A draft only needs a title so it can be found again in My lessons.
    const valid = await trigger("title", { shouldFocus: true })
    if (!valid) {
      setDraftMessage(null)
      return
    }
    // TODO: replace with the save draft API call.
    console.info("Save draft", getValues())
    setDraftMessage("Draft saved. You can finish it later from My lessons.")
  }

  const descriptionLength = values.description?.length ?? 0

  return (
    <form
      noValidate
      onSubmit={handleSubmit(onPublish)}
      className="flex flex-col gap-6 lg:grid lg:grid-cols-5 lg:items-start"
    >
      <div className="col-span-3 flex flex-col gap-6">
        {/* 1. Lesson basics */}
        <Card className="gap-6 px-(--card-spacing)">
          <SectionHeader
            step={1}
            title="Lesson basics"
            description="What you will teach and who it is for."
          />

          <Field data-invalid={!!errors.title} className="gap-1.5">
            <FieldLabel htmlFor={ids.title}>
              Lesson title <Required />
            </FieldLabel>
            <Input
              id={ids.title}
              placeholder="e.g. Intro to TypeScript"
              autoComplete="off"
              aria-invalid={!!errors.title}
              aria-required
              className="h-10"
              {...register("title")}
            />
            <FieldDescription>
              Keep it short. Learners search for this.
            </FieldDescription>
            <FieldError errors={[errors.title]} />
          </Field>

          <Field data-invalid={!!errors.category} className="gap-1.5">
            <FieldLabel htmlFor={ids.category}>
              Category <Required />
            </FieldLabel>
            <Controller
              control={control}
              name="category"
              render={({ field }) => (
                <Select
                  value={field.value ?? null}
                  onValueChange={(value) => field.onChange(value ?? undefined)}
                  onOpenChange={(open) => !open && field.onBlur()}
                >
                  <SelectTrigger
                    id={ids.category}
                    ref={field.ref}
                    aria-invalid={!!errors.category}
                    className="h-10 w-full data-[size=default]:h-10"
                  >
                    <SelectValue placeholder="Pick a category" />
                  </SelectTrigger>
                  <SelectContent>
                    {LESSON_CATEGORIES.map((category) => (
                      <SelectItem key={category} value={category}>
                        {category}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
            <FieldDescription>
              Learners can filter Discover by category.
            </FieldDescription>
            <FieldError errors={[errors.category]} />
          </Field>

          <Field data-invalid={!!errors.description} className="gap-1.5">
            <FieldLabel htmlFor={ids.description}>
              Short description <Required />
            </FieldLabel>
            <textarea
              id={ids.description}
              rows={3}
              maxLength={DESCRIPTION_MAX}
              placeholder="e.g. Types, interfaces and generics, building a small typed utility library."
              aria-invalid={!!errors.description}
              aria-required
              aria-describedby={`${ids.description}-count`}
              className="min-h-24 w-full resize-y rounded-md border border-input bg-transparent px-2.5 py-2 text-base shadow-xs transition-[color,box-shadow] outline-none placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 aria-invalid:border-destructive aria-invalid:ring-3 aria-invalid:ring-destructive/20 md:text-sm dark:bg-input/30 dark:aria-invalid:border-destructive/50 dark:aria-invalid:ring-destructive/40"
              {...register("description")}
            />
            <FieldDescription id={`${ids.description}-count`}>
              <span className="font-heading">
                {descriptionLength} / {DESCRIPTION_MAX}
              </span>{" "}
              · shown on your card in Discover
            </FieldDescription>
            <FieldError errors={[errors.description]} />
          </Field>

          <FieldSet className="gap-1.5" data-invalid={!!errors.level}>
            <FieldLegend variant="label" className="mb-1">
              Level <Required />
            </FieldLegend>
            <div className="flex flex-wrap gap-2">
              {LESSON_LEVELS.map((level) => (
                <label
                  key={level}
                  className="inline-flex h-10 cursor-pointer items-center rounded-md border border-border px-3.5 text-sm font-medium transition-colors hover:bg-muted/50 has-checked:border-primary has-checked:bg-primary/15 has-focus-visible:ring-3 has-focus-visible:ring-ring/50"
                >
                  <input
                    type="radio"
                    value={level}
                    className="sr-only"
                    {...register("level")}
                  />
                  {level}
                </label>
              ))}
            </div>
            <FieldError errors={[errors.level]} />
          </FieldSet>
        </Card>

        {/* 2. Outcomes */}
        <Card className="gap-6 px-(--card-spacing)">
          <SectionHeader
            step={2}
            title="What learners will learn"
            description={`Add one to ${MAX_OUTCOMES} outcomes.`}
          />

          {outcomes.fields.map((item, index) => {
            const error = errors.outcomes?.[index]?.text as
              RHFFieldError | undefined
            const inputId = `outcome-${item.id}`
            return (
              <Field key={item.id} data-invalid={!!error} className="gap-1.5">
                <FieldLabel htmlFor={inputId}>
                  Outcome {index + 1}{" "}
                  {index === 0 ? (
                    <Required />
                  ) : (
                    <span className="font-normal">(optional)</span>
                  )}
                </FieldLabel>
                <div className="flex gap-2">
                  <Input
                    id={inputId}
                    placeholder={OUTCOME_PLACEHOLDERS[index]}
                    autoComplete="off"
                    aria-invalid={!!error}
                    aria-required={index === 0}
                    className="h-10"
                    {...register(`outcomes.${index}.text`)}
                  />
                  {index > 1 && (
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon-lg"
                      onClick={() => outcomes.remove(index)}
                      aria-label={`Remove outcome ${index + 1}`}
                    >
                      <TrashIcon />
                    </Button>
                  )}
                </div>
                <FieldError errors={[error]} />
              </Field>
            )
          })}

          {outcomes.fields.length < MAX_OUTCOMES && (
            <Button
              type="button"
              variant="outline"
              className="h-10 w-fit bg-transparent"
              onClick={() => outcomes.append({ text: "" })}
            >
              <PlusIcon />
              Add outcome
            </Button>
          )}

          <Field data-invalid={!!errors.prerequisites} className="gap-1.5">
            <FieldLabel htmlFor={ids.prerequisites}>
              Prerequisites <span className="font-normal">(optional)</span>
            </FieldLabel>
            <Input
              id={ids.prerequisites}
              placeholder="e.g. Basic JavaScript"
              autoComplete="off"
              aria-invalid={!!errors.prerequisites}
              className="h-10"
              {...register("prerequisites")}
            />
            <FieldError errors={[errors.prerequisites]} />
          </Field>
        </Card>

        {/* 3. Tools and schedule */}
        <Card className="gap-6 px-(--card-spacing)">
          <SectionHeader
            step={3}
            title="When and how you teach"
            description="Choose the tools for your session room. Your time slots come from your calendar."
          />

          <FieldSet className="gap-1.5" data-invalid={!!errors.tools}>
            <FieldLegend variant="label" className="mb-1">
              Session tools <Required />
            </FieldLegend>
            <Controller
              control={control}
              name="tools"
              render={({ field }) => (
                <div className="grid gap-3 sm:grid-cols-2">
                  {SESSION_TOOLS.map((tool) => {
                    const ToolIcon = TOOL_ICONS[tool]
                    const option = SESSION_TOOL_OPTIONS[tool]
                    const checked = field.value?.includes(tool) ?? false
                    return (
                      <label
                        key={tool}
                        className="group flex cursor-pointer items-center gap-3 rounded-lg border border-border p-4 transition-colors hover:bg-muted/50 has-checked:border-primary has-checked:bg-primary/10 has-focus-visible:ring-3 has-focus-visible:ring-ring/50"
                      >
                        <input
                          type="checkbox"
                          className="sr-only"
                          checked={checked}
                          onBlur={field.onBlur}
                          onChange={(event) =>
                            field.onChange(
                              event.target.checked
                                ? [...(field.value ?? []), tool]
                                : (field.value ?? []).filter((t) => t !== tool)
                            )
                          }
                        />
                        <span className="flex size-8 shrink-0 items-center justify-center rounded-md text-muted-foreground group-has-checked:bg-primary group-has-checked:text-primary-foreground">
                          <ToolIcon className="size-4" />
                        </span>
                        <span className="flex flex-col">
                          <span className="text-sm font-medium">
                            {option.label}
                          </span>
                          <span className="text-sm text-muted-foreground">
                            {option.description}
                          </span>
                        </span>
                      </label>
                    )
                  })}
                </div>
              )}
            />
            <FieldDescription>
              Learners can use any tool you turn on during the session.
            </FieldDescription>
            <FieldError errors={[errors.tools as RHFFieldError | undefined]} />
          </FieldSet>

          <div className="flex flex-col gap-2">
            <h3 className="text-sm font-medium">Your availability</h3>
            <AvailabilityList />
          </div>
        </Card>

        <AfterYouPublish />
      </div>

      <aside className="flex flex-col gap-6 lg:sticky lg:top-4 lg:col-span-2">
        <LessonPreview values={values} />
        <PublishChecklist
          steps={steps}
          isSubmitting={isSubmitting}
          onSaveDraft={onSaveDraft}
        />
        {draftMessage && (
          <p role="status" className="text-sm text-muted-foreground">
            {draftMessage}
          </p>
        )}
      </aside>
    </form>
  )
}

function AfterYouPublish() {
  const { earned, cap } = MOCK_WEEKLY_POINTS
  const items: { icon: Icon; title: string; body: React.ReactNode }[] = [
    {
      icon: MagnifyingGlassIcon,
      title: "Learners can find it",
      body: "Your lesson appears in Discover, and people can book your open calendar slots.",
    },
    {
      icon: CurrencyCircleDollarIcon,
      title: "You earn points as you teach",
      body: (
        <>
          Points come from the time you spend teaching, up to your weekly point
          cap.
          <span
            className="mt-2 block h-1.5 overflow-hidden rounded-full bg-muted"
            role="progressbar"
            aria-label="Points earned this week"
            aria-valuemin={0}
            aria-valuemax={cap}
            aria-valuenow={earned}
          >
            <span
              className="block h-full rounded-full bg-primary"
              style={{ width: `${(earned / cap) * 100}%` }}
            />
          </span>
          <span className="mt-1.5 block font-heading text-xs">
            {earned} / {cap} pts this week
          </span>
        </>
      ),
    },
    {
      icon: StarIcon,
      title: "You earn XP",
      body: "+40 XP for each session you teach. Learning earns +60 XP, which raises your level and cap faster.",
    },
  ]

  return (
    <Card className="gap-6 px-(--card-spacing)">
      <SectionHeader
        title="After you publish"
        description="What to expect once your lesson is live."
      />
      <ul className="flex flex-col gap-5">
        {items.map(({ icon: ItemIcon, title, body }) => (
          <li key={title} className="flex gap-3">
            <span className="flex size-8 shrink-0 items-center justify-center rounded-md bg-muted">
              <ItemIcon className="size-4" />
            </span>
            <div className="flex min-w-0 flex-1 flex-col gap-0.5">
              <span className="text-sm font-medium">{title}</span>
              <span className="text-sm text-muted-foreground">{body}</span>
            </div>
          </li>
        ))}
      </ul>
    </Card>
  )
}
