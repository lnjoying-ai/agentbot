# Heartbeat Tasks

This file is checked periodically by your AgentBot assistant.
Add tasks below that you want the agent to work on automatically at regular intervals.

**How it works:**
- AgentBot checks this file every 30 minutes (configurable)
- Tasks under "Active Tasks" will be processed automatically
- Completed tasks should be moved to the "Completed" section
- If this file has no active tasks, the heartbeat will be skipped

**Use cases:**
- Periodic system health checks
- Automated code quality scans
- Regular dependency updates
- Scheduled backups or cleanup
- Monitoring for specific conditions

---

## Active Tasks

<!-- Add your periodic tasks below this line -->

<!-- Example:
### Monitor Build Status
- Check if CI/CD pipeline is passing
- Alert if any tests fail
- Report last successful build time

### Check for Security Updates
- Scan dependencies for known vulnerabilities
- Report any high-severity issues
- Suggest update commands if needed
-->


## Paused Tasks

<!-- Tasks that are temporarily disabled but should not be deleted -->


## Completed

<!-- Move completed tasks here or delete them -->

<!-- Example completed task:
### ✅ Setup Project Documentation (2026-02-16)
- Created README.md with project overview
- Added API documentation
- Setup contributing guidelines
-->


---

## Configuration

**Heartbeat Interval**: 30 minutes (default)
**Max Tasks Per Heartbeat**: 3
**Timeout Per Task**: 5 minutes

**Note**: The agent will only process tasks that are clearly defined and actionable.
Vague requests may be skipped or require clarification.

---

*Last checked: 2026-03-04 19:24:28*
*Next check: 2026-03-04 19:25:28*
