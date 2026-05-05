# Contributing to ShortStop

Thank you for considering contributing to ShortStop! This document provides guidelines and instructions for contributing.

## 🎯 Ways to Contribute

- **Bug Reports**: Found a bug? Open an issue with reproduction steps
- **Feature Requests**: Have an idea? Describe it in an issue
- **Code Contributions**: Submit pull requests for bug fixes or features
- **Documentation**: Improve README, comments, or guides
- **Testing**: Test on different devices and Android versions

## 🚀 Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork**:
   ```bash
   git clone https://github.com/yourusername/ShortStop.git
   cd ShortStop
   ```
3. **Create a branch**:
   ```bash
   git checkout -b feature/your-feature-name
   ```
4. **Make your changes** and test thoroughly
5. **Commit with clear messages**:
   ```bash
   git commit -m "Add: Brief description of changes"
   ```
6. **Push to your fork**:
   ```bash
   git push origin feature/your-feature-name
   ```
7. **Open a Pull Request** on GitHub

## 📝 Code Style

### Kotlin Guidelines

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Keep functions small and focused
- Add comments for complex logic only
- Use Jetpack Compose best practices

### Formatting

- **Indentation**: 4 spaces (no tabs)
- **Line length**: 120 characters max
- **Imports**: Remove unused imports
- **Naming**: camelCase for variables, PascalCase for classes

## 🧪 Testing Requirements

Before submitting a PR:

- [ ] App builds without errors (`./gradlew assembleDebug`)
- [ ] No new warnings introduced
- [ ] Tested on at least one physical device
- [ ] Existing features still work
- [ ] New features have been manually tested

## 🐛 Bug Reports

Include in your issue:

1. **Device**: Model and Android version
2. **Steps to reproduce**: Clear, numbered steps
3. **Expected behavior**: What should happen
4. **Actual behavior**: What actually happens
5. **Screenshots**: If applicable
6. **Logs**: Relevant logcat output

## 💡 Feature Requests

Describe:

1. **Problem**: What issue does this solve?
2. **Solution**: Your proposed feature
3. **Alternatives**: Other solutions you considered
4. **Impact**: Who benefits from this?

## 🔒 Privacy Commitment

All contributions must maintain:

- **No data collection** — Nothing leaves the device
- **No internet usage** — App stays 100% offline
- **No third-party services** — No analytics or tracking
- **Local-only storage** — All data in Room + SQLCipher database
- **No public mutable service state** — Expose state via Flow only

## 📋 Pull Request Checklist

- [ ] Code follows project style guidelines
- [ ] Commit messages are clear and descriptive
- [ ] Changes are tested on a real device
- [ ] No new compiler warnings
- [ ] Privacy principles maintained
- [ ] Documentation updated if needed
- [ ] ProGuard rules updated if needed

## 🚫 What We Won't Accept

- Features requiring internet permission
- Third-party analytics or tracking
- Ads or monetization code
- Breaking changes without discussion
- Code that collects user data

## 📞 Questions?

- Open a GitHub issue for questions
- Tag with `question` label
- We'll respond as soon as possible

## 📜 License

By contributing, you agree that your contributions will be licensed under the same license as the project.

---

**Thank you for helping make ShortStop better!** 🙏
