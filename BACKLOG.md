# FitTrack Backlog

## P1 — 已完成

| ID | 问题 | 文件 | 负责人 | 状态 |
|----|------|------|--------|------|
| P1-1 | `apiService` 动态重建无同步保护 | `QwenRepository.kt:64-81` | @文藏 | ✅ `bc81b8f` |
| P1-2 | `encodeImageToBase64` 大图无尺寸限制，可能 OOM | `QwenRepository.kt:543-559` | @文藏 | ✅ `bc81b8f` |
| P1-3 | `FitTrackRepository` 冗余用户档案代理方法 | `FitTrackRepository.kt:112-203` | @文藏 | ✅ `bc81b8f` |
| P1-4 | `Transition.kt` 缺少 `IntOffset` import | `Transition.kt` | @佐佐木 | ✅ `4a18713` |
| P1-5 | `HomeScreen.kt` 缺少 `NavigationBarItem` import | `HomeScreen.kt` | @佐佐木 | ✅ `4a18713` |
| P1-6 | `HomeScreen.NavItem` 缺少 `RowScope` 接收者导致 `NavigationBarItem` 无法解析 | `HomeScreen.kt:321` | @小花 | ✅ 已修复 |

## P2 — 下一迭代

### BUG-001: 通知 ID 碰撞风险

- **文件**: `WorkoutReminderWorker.kt:165`
- **问题**: `System.currentTimeMillis().toInt()` 作为通知 ID，同一毫秒内触发多个 Worker 会覆盖通知
- **严重度**: 低
- **修复建议**: 使用 `planId.hashCode()` 或 `(planId shl 16) or dayOfWeek` 生成唯一 ID
- **负责人**: @文藏
- **标签**: `bug`, `reminder`

### BUG-002: 整数除法精度丢失

- **文件**: `PressureAnalyzer.kt:195`
- **问题**: `(lowEnergyCount * 20 + lowSleepCount * 25) / recentRecords.size` 整数除法，结果永远为整数，精度丢失
- **严重度**: 低
- **修复建议**: 改为 `.toFloat()` 后再除，或将乘数和除法顺序调整避免截断：`(lowEnergyCount * 20 + lowSleepCount * 25).toFloat() / recentRecords.size`
- **负责人**: @文藏
- **标签**: `bug`, `analyzer`

### BUG-003: `extractJson` 提取策略脆弱

- **文件**: `QwenRepository.kt:714-724`
- **问题**: `indexOf('{')` + `lastIndexOf('}')` 在 AI 返回包含多个花括号对（如嵌套 JSON）时可能提取到多余内容，或 AI 输出中混入非 JSON 文本时产生错误解析
- **严重度**: 低（当前作为 fallback 策略可接受）
- **修复建议**:
  1. 优先尝试 JSON 反序列化整个响应，失败时再用字符串提取作为 fallback
  2. 增加括号匹配计数器，确保提取的是最外层的 `{...}` 块
  3. 提取后尝试 `Gson.fromJson` 验证有效性
- **负责人**: @文藏
- **标签**: `tech-debt`, `qwen`

## P3 — 后续迭代储备

| ID | 问题 | 来源 | 备注 |
|----|------|------|------|
| P3-1 | `ChatViewModel.imageUriToBase64` 无 OOM 防护 | 品控建议 | 与 P1-2 同类问题，content:// URI 图片也可能很大 |
| P3-2 | `encodeImageToBase64` 的 `inSampleSize` 循环条件用整数除法不够精确 | 品控建议 | 边界条件：width=2049, inSampleSize=2 时提前退出 |
| P3-3 | `QwenRepository` 大量重复的 API 调用错误处理模式 | 品控建议 | 建议抽取 `executeApiCall<T>` 通用方法 |
| P3-4 | `FitTrackRepository` 存在重复的同功能方法名（getRecordsByPlan/getRecordsForPlan、insertRecord/insertWorkoutRecord） | 品控建议 | P1-3 清理后可能仍残留 |
| P3-5 | `SettingsManager` 加密降级时无用户提示 | 品控建议 | 回退到明文 SharedPreferences 时应提示用户 |
| P3-6 | `backup_rules.xml` 全量导出数据库和 SharedPreferences | 品控建议 | 建议仅备份必要数据，排除加密密钥相关文件 |
| P3-7 | `WorkoutScheduler.getTodayExercises` 多计划场景只取第一个计划 | 品控建议 | 应遍历所有不同 planId 的日程 |
| P3-8 | `StatisticsScreen` 周训练概览使用硬编码假数据、月度目标不可配置 | 品控建议 | 需接入真实数据，佐佐木负责 |
