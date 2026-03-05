# Long-term Memory

This file stores important information that should persist across sessions.
AgentBot automatically loads this content into every conversation as context.

**What to store here:**
- Important facts about the user and their preferences
- Project context and architectural decisions
- Lessons learned from past interactions
- Frequently used patterns and best practices
- Important reminders and notes

**What NOT to store here:**
- Temporary session data (use memory.log instead)
- Sensitive information (passwords, API keys)
- Very detailed code (store in actual project files)

---

## User Information

<!-- Important facts about the user -->

**Name**: 
**Role**: 
**Expertise Level**: Expert
**Preferred Language**: 简体中文 (Simplified Chinese)

### Communication Preferences
- Prefers concise, technical explanations
- Appreciates proactive suggestions
- Values safety and verification before destructive operations


## Project Context

<!-- Information about ongoing projects -->

### Current Project: AgentBot
**Type**: AI-powered coding assistant with Spring Boot backend
**Tech Stack**: Java 17, Spring Boot 3.x, LLM integration
**Architecture**: Event-driven with tool-based agent system
**Key Features**:
- Multi-round conversation with tool calling
- Approval mechanism for dangerous operations
- Session-based memory and context management
- Plugin system with skills and agents

### Recent Work
- Implemented AGENTS.md documentation system (2026-02-16)
- Created SOUL.md to define AI personality (2026-02-16)
- Fixed confirmation card display issues in frontend
- Enhanced tool approval workflow


## Preferences

<!-- User preferences learned over time -->

### Code Style
- Clean, maintainable code over clever tricks
- Proper error handling and logging
- Comprehensive comments for complex logic
- Follow Spring Boot best practices

### Workflow
- Test changes before committing
- Keep commits atomic and well-described
- Use Git for version control
- Prefer incremental improvements over big rewrites

### Tools & Technologies
- **IDE**: Unknown
- **OS**: Windows 32-bit
- **Shell**: PowerShell
- **Version Control**: Git


## Important Notes

<!-- Things to remember -->

### Safety Rules
1. Always explain destructive operations before executing
2. Never bypass the approval mechanism
3. Verify file paths before deletion or modification
4. Keep backups of important configurations

### Project Structure
- `workspace/` contains agent guidelines, skills, and memory
- `workspace/SOUL.md` defines AI personality (highest priority)
- `workspace/AGENTS.md` defines behavior rules
- `workspace/TOOLS.md` contains tool documentation
- `workspace/memory/` stores long-term and daily memory

### Known Issues
- None currently tracked

### Future Improvements
- Implement HEARTBEAT service for periodic tasks
- Add more sophisticated memory search capabilities
- Enhance skill system with dynamic loading
- Improve frontend confirmation UX


## Learning & Insights

<!-- Lessons learned from past interactions -->

### Effective Patterns
- **Documentation as Policy**: Using Markdown files (AGENTS.md, SOUL.md) to guide AI behavior
- **Session-aware Tools**: Every tool execution is tied to a specific session context
- **Two-round Approval**: LLM requests → System checks → User approves → Tool executes → LLM continues
- **Metadata-driven UI**: Frontend renders confirmation cards based on `metadata.status = "PENDING_APPROVAL"`

### Common Mistakes to Avoid
- Don't re-request approval after user confirms (leads to loops)
- Don't split dangerous commands to bypass approval
- Always read files before editing to get current state
- Keep memory files concise (avoid token bloat)


## Session Statistics

**Total Sessions**: (To be tracked)
**Most Common Tasks**: (To be analyzed)
**Success Rate**: (To be calculated)


---

## Maintenance

**Last Updated**: 2026-02-16
**Updated By**: AgentBot initialization
**Next Review**: 2026-03-16 (monthly review recommended)

---

*This file is automatically loaded into every conversation. Keep it concise and relevant.*
*AgentBot can update this file using the memory_append tool when instructed to remember something important.*
