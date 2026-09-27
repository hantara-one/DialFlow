# 📞 DialFlow

> An Android calling queue app designed to make repetitive customer-calling workflows faster, more organized, and easier to manage.

**DialFlow** is an Android application built to streamline workflows that involve calling a large number of phone numbers.

Instead of manually dialing numbers one by one, DialFlow transforms phone numbers into a structured, manageable queue and provides tools to navigate, track, search, and revisit numbers throughout the calling process.

---

## ✨ Features

### 📋 Calling Queue

- Import multiple phone numbers at once.
- Automatically organize numbers into a structured calling queue.
- Navigate through the queue sequentially.
- Reorder numbers using **drag-and-drop**.
- Easily return to previously handled numbers.
- Clear the entire queue when needed.

### 📞 Calling Modes

Choose the calling workflow that fits your needs:

- **Auto** — automatically proceeds through the queue with a configurable cooldown.
- **Manual** — gives the user full control over when to proceed to the next number.

### 📊 Call Status & Progress Tracking

Each number can have one of the following statuses:

- **Pending** — The number has not been processed yet.
- **In Progress** — The number is currently being handled.
- **Called** — The number has been called.
- **Skipped** — The number has been intentionally skipped.

> Status represents call history and does not prevent a number from being selected or called again.

Queue progress is based on **processed numbers**, meaning both **Called** and **Skipped** numbers contribute to the overall progress.

### 🔎 Queue Search

Find and manage numbers quickly within a large queue.

- Search for specific phone numbers.
- Numeric phone keyboard for faster input.
- **Move to Number** — quickly jump to a matching number in the queue.
- **Set as Current** — select a number as the current calling target.
- **Delete** — remove a specific number from the queue.
- Search results can be used to navigate directly to queue items.

### 🧭 Queue Navigation

- **Return** to previously handled numbers.
- **Skip** numbers when necessary.
- **Back to Top** button for quickly returning to the beginning of a long queue.
- Current queue item is visually highlighted.
- Navigation remains usable across different Android navigation modes.

### 📱 Phone Number Formatting

DialFlow supports flexible phone number formatting:

- **Local** — `08...`
- **International** — `+62...`

The selected format affects both the number displayed in the queue and the number used when initiating a call.

Original pasted input is preserved as a raw reference.

### 🌐 Language Support

DialFlow currently supports:

- 🇬🇧 **English**
- 🇮🇩 **Bahasa Indonesia**

Language preferences can be changed directly from the Settings screen.

### 🎨 Appearance

Customize the application's appearance with:

- ☀️ **Light**
- 🌙 **Dark**
- 📱 **System Default**

The Dark theme uses a dedicated color palette while preserving DialFlow's purple visual identity.

### ⚙️ Settings

DialFlow provides configurable preferences for:

- 🌐 Application language
- 📱 Phone number format
- 🎨 Appearance / theme

User preferences are preserved between application sessions.

---

## 💡 Why DialFlow?

DialFlow was created to solve a simple but repetitive problem:

> **Calling a large number of people efficiently without repeatedly entering phone numbers manually.**

When hundreds of phone numbers need to be contacted, manually entering each number becomes unnecessarily time-consuming and tiring.

DialFlow turns those numbers into a structured queue, allowing the user to focus on the actual conversation instead of repeatedly entering phone numbers.

The project started as a practical tool for a real-world calling workflow and gradually evolved into a more polished Android application.

---

## 🔄 How It Works

The basic workflow is simple:

1. 📋 **Import** a list of phone numbers.
2. 📑 DialFlow creates a **calling queue**.
3. ⚙️ Select **Auto** or **Manual** calling mode.
4. 📞 Call numbers directly from the queue.
5. 📊 Track each number using its status and queue progress.
6. 🔎 Use **Search**, **Move to Number**, or **Set as Current** when needed.
7. ⏭️ Use **Return** or **Skip** to manage the queue as you work.

---

## 🛠️ Development

DialFlow is developed as an Android application with the assistance of **Google AI Studio** and **Android Studio**.

The project began as an AI-assisted development experiment and evolved into a functional tool designed around an actual high-volume calling workflow.

Development focuses on:

- ⚡ Workflow efficiency
- 📱 Android usability
- 🎨 Clean and practical UI
- 🧩 Simple queue management
- 🔄 Reliable calling navigation

---

## 📌 Project Status

**🟢 Stable — v1.3.0**

DialFlow is currently functional and stable for its intended calling workflow.

Version **1.3.0** introduces several workflow and UX improvements, including:

- 📊 Improved queue progress tracking
- ⏭️ Skipped items contributing to progress
- 🌙 Light / Dark / System Default themes
- 📱 Improved Android 3-button navigation compatibility
- ↕️ Queue reordering
- 📞 Phone number format preferences
- 🇮🇩 Indonesian language support
- 🎨 Various UI and UX refinements

Future development may include additional workflow optimizations, usability improvements, and further refinement of the Android experience.

---

## 📸 Screenshots

<table>
  <tr>
    <td align="center">
      <img width="720" height="1600" alt="Import Numbers" src="https://github.com/user-attachments/assets/c655f240-1179-404f-a1c9-1abc2f5665c7" /> <br>
      <b>Import Numbers</b>
    </td>
    <td align="center">
      <img width="720" height="1600" alt="Calling Queue" src="https://github.com/user-attachments/assets/9b3ecd34-f148-43ce-bf28-3a99509e4f61" /> <br>
      <b>Calling Queue</b>
    </td>
    <td align="center">
      <img width="720" height="1600" alt="Progress Tracking" src="https://github.com/user-attachments/assets/7b3ea5df-d285-404c-8144-378bf11c6e8e" /> <br>
      <b>Progress Tracking</b>
    </td>
    <td align="center">
      <img width="720" height="1600" alt="Settings" src="https://github.com/user-attachments/assets/fecf0680-5f25-463c-95ee-4550cd13233f" /> <br>
      <b>Settings</b>
    </td>
  </tr>
</table>

---

## 📦 Releases

Check the **[Releases](https://github.com/hantara-one/DialFlow/releases)** section of this repository for downloadable APK builds and version history.

---

## 📄 License

License information will be added in a future update.

---

<p align="center">
  <b>DialFlow</b><br>
  A Queue Manager by AM Hanif
</p>
