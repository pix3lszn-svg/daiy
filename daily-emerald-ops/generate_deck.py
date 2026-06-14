#!/usr/bin/env python3
"""Generate the kickoff meeting slide deck for The Daily Emerald operating structure proposal.

Design is intentionally plain and minimal so it can be styled later.
No em dashes anywhere. UTC for times. [Name TBD] placeholders.
"""

from pptx import Presentation
from pptx.util import Inches, Pt, Emu
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE

# Plain, minimal palette
INK = RGBColor(0x1A, 0x1A, 0x1A)
MUTED = RGBColor(0x5A, 0x5A, 0x5A)
ACCENT = RGBColor(0x0B, 0x6E, 0x4F)  # emerald, restrained
LINE = RGBColor(0xC8, 0xC8, 0xC8)
BOXFILL = RGBColor(0xF2, 0xF4, 0xF3)
WHITE = RGBColor(0xFF, 0xFF, 0xFF)

prs = Presentation()
prs.slide_width = Inches(13.333)
prs.slide_height = Inches(7.5)
SW = prs.slide_width
SH = prs.slide_height
BLANK = prs.slide_layouts[6]


def slide():
    return prs.slides.add_slide(BLANK)


def textbox(s, left, top, width, height):
    tb = s.shapes.add_textbox(left, top, width, height)
    tf = tb.text_frame
    tf.word_wrap = True
    return tb, tf


def set_run(run, text, size, bold=False, color=INK, italic=False):
    run.text = text
    f = run.font
    f.size = Pt(size)
    f.bold = bold
    f.italic = italic
    f.color.rgb = color
    f.name = "Calibri"


def add_para(tf, text, size, bold=False, color=INK, italic=False, bullet=False,
             space_after=8, align=PP_ALIGN.LEFT, first=False):
    p = tf.paragraphs[0] if first and not tf.paragraphs[0].runs else tf.add_paragraph()
    p.alignment = align
    p.space_after = Pt(space_after)
    r = p.add_run()
    prefix = "•  " if bullet else ""
    set_run(r, prefix + text, size, bold=bold, color=color, italic=italic)
    return p


def accent_bar(s, top=Inches(1.15)):
    bar = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.7), top, Inches(2.2), Pt(3))
    bar.fill.solid()
    bar.fill.fore_color.rgb = ACCENT
    bar.line.fill.background()
    return bar


def title_only(s, title, kicker=None):
    if kicker:
        _, ktf = textbox(s, Inches(0.7), Inches(0.45), Inches(11.9), Inches(0.4))
        add_para(ktf, kicker.upper(), 12, bold=True, color=ACCENT, first=True, space_after=0)
        _, ttf = textbox(s, Inches(0.7), Inches(0.72), Inches(11.9), Inches(0.9))
        add_para(ttf, title, 30, bold=True, color=INK, first=True, space_after=0)
        accent_bar(s, Inches(1.5))
    else:
        _, ttf = textbox(s, Inches(0.7), Inches(0.5), Inches(11.9), Inches(0.9))
        add_para(ttf, title, 30, bold=True, color=INK, first=True, space_after=0)
        accent_bar(s)


def box(s, left, top, width, height, text, fill=BOXFILL, txt_color=INK,
        size=12, bold=False, line_color=LINE, sub=None):
    shp = s.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, left, top, width, height)
    shp.fill.solid()
    shp.fill.fore_color.rgb = fill
    shp.line.color.rgb = line_color
    shp.line.width = Pt(0.75)
    tf = shp.text_frame
    tf.word_wrap = True
    tf.vertical_anchor = MSO_ANCHOR.MIDDLE
    tf.margin_top = Pt(3)
    tf.margin_bottom = Pt(3)
    p = tf.paragraphs[0]
    p.alignment = PP_ALIGN.CENTER
    r = p.add_run()
    set_run(r, text, size, bold=bold, color=txt_color)
    if sub:
        p2 = tf.add_paragraph()
        p2.alignment = PP_ALIGN.CENTER
        r2 = p2.add_run()
        set_run(r2, sub, size - 2, color=txt_color)
    return shp


def footer(s, n):
    _, ftf = textbox(s, Inches(0.7), Inches(7.05), Inches(9), Inches(0.35))
    add_para(ftf, "The Daily Emerald  |  Operating Structure Proposal  |  For discussion, not final",
             9, color=MUTED, first=True, space_after=0)
    _, ptf = textbox(s, Inches(12.3), Inches(7.05), Inches(0.8), Inches(0.35))
    add_para(ptf, str(n), 9, color=MUTED, first=True, space_after=0, align=PP_ALIGN.RIGHT)


# ---------------------------------------------------------------- Slide 1: Cover
s = slide()
band = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, SW, Inches(0.18))
band.fill.solid(); band.fill.fore_color.rgb = ACCENT; band.line.fill.background()
_, tf = textbox(s, Inches(0.9), Inches(2.2), Inches(11.5), Inches(1.4))
add_para(tf, "THE DAILY EMERALD", 16, bold=True, color=ACCENT, first=True, space_after=4)
add_para(tf, "Operating Structure", 44, bold=True, color=INK, space_after=0)
add_para(tf, "A Proposal for the Team", 44, bold=True, color=INK, space_after=0)
_, tf2 = textbox(s, Inches(0.9), Inches(4.4), Inches(11.5), Inches(1.2))
add_para(tf2, "This is a proposal, not a final decision. Nothing here is locked.",
         18, color=MUTED, first=True, space_after=4)
add_para(tf2, "Your feedback will shape the version we actually adopt.",
         18, bold=True, color=INK, space_after=0)
_, tf3 = textbox(s, Inches(0.9), Inches(6.6), Inches(11.5), Inches(0.5))
add_para(tf3, "Kickoff meeting  |  Presented by The Daily Emerald  |  Times shown in UTC",
         12, color=MUTED, first=True, space_after=0)

# ---------------------------------------------------------------- Slide 2: Why
s = slide()
title_only(s, "Why we are doing this", kicker="The problem we are fixing")
_, tf = textbox(s, Inches(0.7), Inches(1.9), Inches(11.9), Inches(4.5))
add_para(tf, "Until now, The Daily Emerald has run on one person. That worked when it was one person.",
         18, first=True, space_after=14)
add_para(tf, "Right now, every decision waits on me. That makes me the bottleneck, and it slows all of you down.",
         18, space_after=14)
add_para(tf, "The fix is structure. Clear teams, clear leaders, and a clear way work moves, so the brand can grow without everything routing through a single inbox.",
         18, space_after=14)
add_para(tf, "This deck lays out a proposed structure. We will talk through it together, and your changes will be folded in before anything is final.",
         18, bold=True, color=ACCENT, space_after=0)
footer(s, 2)

# ---------------------------------------------------------------- Slide 3: At a glance
s = slide()
title_only(s, "The proposal at a glance", kicker="What we are proposing")
items = [
    ("Two divisions, eight teams", "Every staff group gets its own team and its own lead."),
    ("A single line up the chain", "Members to Team Lead to Division Head to Founder. I coordinate two people, not forty."),
    ("A weekly operating rhythm", "Plan, assign, execute, review. A predictable cadence everyone can rely on."),
    ("One home: Discord", "All internal work lives in Discord. No second tool, and no AI tools."),
    ("Recognition and ownership", "Real titles, real responsibility, and credit for the people doing the work."),
    ("An open question to decide together", "Whether to add a third Division Head. That is yours to weigh in on."),
]
gx, gy = Inches(0.7), Inches(1.85)
cw, ch = Inches(5.85), Inches(1.55)
for i, (h, d) in enumerate(items):
    col = i % 2
    row = i // 2
    left = gx + col * (cw + Inches(0.3))
    top = gy + row * (ch + Inches(0.22))
    shp = box(s, left, top, cw, ch, "", fill=BOXFILL)
    tf = shp.text_frame
    tf.vertical_anchor = MSO_ANCHOR.TOP
    tf.margin_left = Pt(10); tf.margin_right = Pt(10); tf.margin_top = Pt(8)
    p = tf.paragraphs[0]; p.alignment = PP_ALIGN.LEFT
    r = p.add_run(); set_run(r, h, 15, bold=True, color=ACCENT)
    p2 = tf.add_paragraph(); p2.alignment = PP_ALIGN.LEFT
    r2 = p2.add_run(); set_run(r2, d, 12, color=INK)
footer(s, 3)

# ---------------------------------------------------------------- Slide 4: Divisions and teams
s = slide()
title_only(s, "Two divisions, eight teams", kicker="The organisation")
# Division 1
box(s, Inches(0.7), Inches(1.9), Inches(5.9), Inches(0.7),
    "Creative and Production", fill=ACCENT, txt_color=WHITE, size=18, bold=True, line_color=ACCENT)
d1 = ["Builders", "Artists", "Content Team", "Voice Actors"]
for i, t in enumerate(d1):
    box(s, Inches(0.7), Inches(2.75) + i * Inches(0.92), Inches(5.9), Inches(0.78),
        t, size=15, bold=True)
# Division 2
box(s, Inches(6.75), Inches(1.9), Inches(5.9), Inches(0.7),
    "Live Operations", fill=ACCENT, txt_color=WHITE, size=18, bold=True, line_color=ACCENT)
d2 = ["Event Organisers", "Broadcast Team", "Moderation Team", "Wiki Admin"]
for i, t in enumerate(d2):
    box(s, Inches(6.75), Inches(2.75) + i * Inches(0.92), Inches(5.9), Inches(0.78),
        t, size=15, bold=True)
footer(s, 4)

# ---------------------------------------------------------------- Slide 5: Chain of command
s = slide()
title_only(s, "The chain of command", kicker="One clean line, top to bottom")
cx = Inches(4.9); cw = Inches(3.5)
box(s, cx, Inches(1.75), cw, Inches(0.65), "Founder / Director", fill=ACCENT, txt_color=WHITE, size=15, bold=True, line_color=ACCENT,
    sub=None)
box(s, Inches(2.6), Inches(2.85), Inches(3.6), Inches(0.65), "Division Head", size=14, bold=True)
box(s, Inches(7.1), Inches(2.85), Inches(3.6), Inches(0.65), "Division Head", size=14, bold=True)
box(s, Inches(2.6), Inches(3.95), Inches(3.6), Inches(0.6), "4 Team Leads", size=13, bold=True)
box(s, Inches(7.1), Inches(3.95), Inches(3.6), Inches(0.6), "4 Team Leads", size=13, bold=True)
box(s, Inches(2.6), Inches(4.95), Inches(3.6), Inches(0.6), "Team Members", fill=WHITE, size=13)
box(s, Inches(7.1), Inches(4.95), Inches(3.6), Inches(0.6), "Team Members", fill=WHITE, size=13)
_, tf = textbox(s, Inches(0.7), Inches(5.85), Inches(11.9), Inches(1.0))
add_para(tf, "Each Team Lead is the only person who talks to their Division Head. Each Division Head is the only person who talks to me.",
         16, bold=True, first=True, space_after=4)
add_para(tf, "That single line is what stops the founder being the bottleneck.", 14, color=MUTED, space_after=0)
footer(s, 5)

# ---------------------------------------------------------------- Slide 6: Leadership requirements
s = slide()
title_only(s, "Leadership requirements", kicker="For Team Leads and Division Heads")
_, tf = textbox(s, Inches(0.7), Inches(1.9), Inches(11.9), Inches(0.7))
add_para(tf, "Two simple requirements for anyone leading a group, the eight Team Leads and the Division Heads above them.",
         16, first=True, space_after=0)
box(s, Inches(0.9), Inches(2.95), Inches(5.6), Inches(2.0),
    "", fill=BOXFILL)
b1 = s.shapes[-1].text_frame
b1.vertical_anchor = MSO_ANCHOR.MIDDLE
p = b1.paragraphs[0]; p.alignment = PP_ALIGN.CENTER
set_run(p.add_run(), "18 or older", 24, bold=True, color=ACCENT)
p2 = b1.add_paragraph(); p2.alignment = PP_ALIGN.CENTER
set_run(p2.add_run(), "All group leaders must be at least 18 years of age.", 14)
box(s, Inches(6.85), Inches(2.95), Inches(5.6), Inches(2.0), "", fill=BOXFILL)
b2 = s.shapes[-1].text_frame
b2.vertical_anchor = MSO_ANCHOR.MIDDLE
p = b2.paragraphs[0]; p.alignment = PP_ALIGN.CENTER
set_run(p.add_run(), "Working microphone", 24, bold=True, color=ACCENT)
p2 = b2.add_paragraph(); p2.alignment = PP_ALIGN.CENTER
set_run(p2.add_run(), "Leadership coordination happens in voice meetings.", 14)
_, tf2 = textbox(s, Inches(0.7), Inches(5.3), Inches(11.9), Inches(0.6))
add_para(tf2, "These keep coordination smooth. They are about the role, not about the person.", 14, color=MUTED, first=True, space_after=0)
footer(s, 6)

# ---------------------------------------------------------------- Slide 7: Open question, third Division Head
s = slide()
title_only(s, "Open question: a third Division Head?", kicker="To decide together in this meeting")
box(s, Inches(0.7), Inches(1.85), Inches(11.9), Inches(0.6), "This is not decided. It is a question for the team.",
    fill=ACCENT, txt_color=WHITE, size=16, bold=True, line_color=ACCENT)
_, tf = textbox(s, Inches(0.7), Inches(2.75), Inches(11.9), Inches(3.8))
add_para(tf, "As proposed, two Division Heads each oversee four teams. That may be too much for two people.",
         16, first=True, space_after=12)
add_para(tf, "Option on the table: add a third Division Head, then regroup the eight teams across three divisions instead of two.",
         16, space_after=12, bold=True)
add_para(tf, "If we go this way, we work out the exact split together. No grouping is fixed in advance.",
         16, space_after=12)
add_para(tf, "Questions for the room: Do three divisions make sense? How would you split the eight teams? Who might step up?",
         16, color=ACCENT, bold=True, space_after=0)
footer(s, 7)

# ---------------------------------------------------------------- Slide 8: Weekly rhythm
s = slide()
title_only(s, "The weekly operating rhythm", kicker="A predictable cadence")
steps = [
    ("Start of week", "Founder sets priorities and creative direction, briefs the Division Heads."),
    ("Translate to tasks", "Division Heads turn priorities into tasks and assign them through Team Leads."),
    ("Mid week execution", "Teams work their tasks with visible status. Individual tasks do not route to the founder."),
    ("Single threaded escalation", "Anything needing the founder is batched by the Heads into one digest."),
    ("All hands alignment", "One or two all hands meetings per week keep everyone aligned."),
    ("End of week review", "Review feeds straight into next week's planning."),
]
gx, gy = Inches(0.7), Inches(1.9)
cw, ch = Inches(5.85), Inches(1.45)
for i, (h, d) in enumerate(steps):
    col = i % 2; row = i // 2
    left = gx + col * (cw + Inches(0.3))
    top = gy + row * (ch + Inches(0.2))
    shp = box(s, left, top, cw, ch, "", fill=BOXFILL)
    tf = shp.text_frame; tf.vertical_anchor = MSO_ANCHOR.TOP
    tf.margin_left = Pt(10); tf.margin_top = Pt(7)
    p = tf.paragraphs[0]; p.alignment = PP_ALIGN.LEFT
    set_run(p.add_run(), f"{i+1}.  {h}", 15, bold=True, color=ACCENT)
    p2 = tf.add_paragraph(); p2.alignment = PP_ALIGN.LEFT
    set_run(p2.add_run(), d, 12, color=INK)
footer(s, 8)

# ---------------------------------------------------------------- Slide 9: How work flows
s = slide()
title_only(s, "How work flows: the task lifecycle", kicker="One forum channel per team")
stages = ["To Do", "In Progress", "Needs Review", "Blocked", "Done"]
bw = Inches(2.3); gap = Inches(0.15); start = Inches(0.7); top = Inches(2.2)
for i, st in enumerate(stages):
    box(s, start + i * (bw + gap), top, bw, Inches(0.95), st, size=14, bold=True,
        fill=(ACCENT if st == "Done" else BOXFILL), txt_color=(WHITE if st == "Done" else INK),
        line_color=(ACCENT if st == "Done" else LINE))
_, tf = textbox(s, Inches(0.7), Inches(3.6), Inches(11.9), Inches(3.0))
add_para(tf, "Every task is a post in its team's Discord forum channel, tagged with a status and assigned to a person.",
         16, first=True, space_after=12)
add_para(tf, "Anyone can see what is in flight, what is stuck, and what is finished, without asking.", 16, space_after=12)
add_para(tf, "Team Leads keep their board healthy. Division Heads watch across their teams. The founder does not touch individual tasks.",
         16, space_after=0, color=MUTED)
footer(s, 9)

# ---------------------------------------------------------------- Slide 10: Meeting format
s = slide()
title_only(s, "The weekly all hands format", kicker="How our meetings run")
steps = [
    "Everyone is muted at the start.",
    "Founder presents the week's plan and any new documentation.",
    "The floor opens for questions and proposed changes.",
    "We converge on agreement, then move to work.",
]
gy = Inches(2.0)
for i, st in enumerate(steps):
    box(s, Inches(0.9), gy + i * Inches(1.0), Inches(0.8), Inches(0.8), str(i+1),
        fill=ACCENT, txt_color=WHITE, size=20, bold=True, line_color=ACCENT)
    _, tf = textbox(s, Inches(1.95), gy + i * Inches(1.0) + Inches(0.12), Inches(10.5), Inches(0.8))
    add_para(tf, st, 18, first=True, space_after=0)
footer(s, 10)

# ---------------------------------------------------------------- Slide 11: Tools
s = slide()
title_only(s, "Tools: Discord only, and AI free", kicker="Where things live")
box(s, Inches(0.7), Inches(1.9), Inches(5.9), Inches(0.65), "Discord is home base",
    fill=ACCENT, txt_color=WHITE, size=16, bold=True, line_color=ACCENT)
_, tf = textbox(s, Inches(0.8), Inches(2.7), Inches(5.7), Inches(3.6))
for t in ["Real time coordination and task work.",
          "One forum channel per team for tasks.",
          "Handbook and weekly plan in a read only channel.",
          "Leadership only channel for escalation.",
          "No second internal tool to learn."]:
    add_para(tf, t, 14, bullet=True, first=(t.startswith("Real")), space_after=8)
box(s, Inches(6.75), Inches(1.9), Inches(5.9), Inches(0.65), "The brand wiki is separate",
    fill=ACCENT, txt_color=WHITE, size=16, bold=True, line_color=ACCENT)
_, tf2 = textbox(s, Inches(6.85), Inches(2.7), Inches(5.7), Inches(3.6))
for t in ["Public facing Daily Emerald wiki.",
          "For lore and brand content, not internal ops.",
          "Owned and maintained by Wiki Admin.",
          "Contains no AI."]:
    add_para(tf2, t, 14, bullet=True, first=(t.startswith("Public")), space_after=8)
_, tf3 = textbox(s, Inches(0.7), Inches(6.35), Inches(11.9), Inches(0.6))
add_para(tf3, "No AI bundled tools are used anywhere in the internal stack.", 14, bold=True, color=ACCENT, first=True, space_after=0)
footer(s, 11)

# ---------------------------------------------------------------- Slide 12: Worked example
s = slide()
title_only(s, "Worked example: Nether vs End", kicker="The structure end to end")
_, tf = textbox(s, Inches(0.7), Inches(1.8), Inches(11.9), Inches(0.7))
add_para(tf, "A scripted exhibition match next week that ends in a staged in lore catastrophe, our in universe reason to delay the Overworld Cup by a week. Fictional entertainment content.",
         14, first=True, space_after=0)
flow = [
    ("Event Organisers", "Plan and schedule the match."),
    ("Builders", "Construct the arena."),
    ("Artists", "Produce promo graphics."),
    ("Content Team", "Script match coverage and the disaster bulletin."),
    ("Voice Actors", "Record the in lore bulletin."),
    ("Broadcast Team", "Cast the match live."),
    ("Moderation Team", "Keep the server orderly."),
    ("Wiki Admin", "Record the event and aftermath on the wiki."),
]
gx, gy = Inches(0.7), Inches(2.65)
cw, ch = Inches(5.85), Inches(0.95)
for i, (h, d) in enumerate(flow):
    col = i % 2; row = i // 2
    left = gx + col * (cw + Inches(0.3)); top = gy + row * (ch + Inches(0.12))
    shp = box(s, left, top, cw, ch, "", fill=BOXFILL)
    tf = shp.text_frame; tf.vertical_anchor = MSO_ANCHOR.MIDDLE
    tf.margin_left = Pt(10)
    p = tf.paragraphs[0]; p.alignment = PP_ALIGN.LEFT
    set_run(p.add_run(), h + ":  ", 13, bold=True, color=ACCENT)
    set_run(p.add_run(), d, 13, color=INK)
footer(s, 12)

# ---------------------------------------------------------------- Slide 13: Teaser
s = slide()
title_only(s, "What getting organised unlocks", kicker="The bigger picture")
_, tf = textbox(s, Inches(0.7), Inches(2.0), Inches(11.9), Inches(3.8))
add_para(tf, "There are much bigger plans for The Daily Emerald.", 22, bold=True, first=True, space_after=16)
add_para(tf, "Bigger events. Sharper production. A real brand with a real audience.", 18, color=MUTED, space_after=16)
add_para(tf, "All of it depends on one thing first: getting organised so we can move as a team instead of a queue.",
         18, space_after=16)
add_para(tf, "Get this right, and the ceiling goes way up.", 18, bold=True, color=ACCENT, space_after=0)
footer(s, 13)

# ---------------------------------------------------------------- Slide 14: Closing
s = slide()
band = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, SW, Inches(0.18))
band.fill.solid(); band.fill.fore_color.rgb = ACCENT; band.line.fill.background()
_, tf = textbox(s, Inches(0.9), Inches(1.6), Inches(11.5), Inches(1.0))
add_para(tf, "This is a proposal", 40, bold=True, color=INK, first=True, space_after=0)
_, tf2 = textbox(s, Inches(0.9), Inches(2.9), Inches(11.5), Inches(3.0))
add_para(tf2, "Nothing here is final. The Discussion Draft is a living document.", 20, first=True, space_after=14)
add_para(tf2, "Your suggestions will be taken into account and folded into the plan.", 20, bold=True, color=ACCENT, space_after=14)
add_para(tf2, "Next steps: we mark up the draft together today, I update it to reflect what we agree, and the final documents are built from that approved version.",
         18, color=MUTED, space_after=0)
_, tf3 = textbox(s, Inches(0.9), Inches(6.4), Inches(11.5), Inches(0.6))
add_para(tf3, "Thank you. The floor is yours.", 18, bold=True, first=True, space_after=0)

prs.save("/home/user/daiy/daily-emerald-ops/Daily-Emerald-Operations-Overview.pptx")
print("Saved deck with", len(prs.slides._sldIdLst), "slides")
