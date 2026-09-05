# MoreAnimation × 官方 YSM 2.6.5：单动作验证版

> 2026-09-05 启动修复更新：下文记录的是原 PoC，现已停用其 Mixin 注册及自动事件订阅。LoadTrace 证实官方 YSM `MixinTweaker.<init>` 在配置选择阶段加载目标类，导致 MoreAnimation PREPARE 报 `MixinTargetAlreadyLoadedException`。本次只恢复启动，不启用替代动画入口，也不扩展动作。当前构建不提供 YSM circledance。

当前结果：已在指定项目实现 `circledance` PoC，实际构建成功；尚未通过 Minecraft 客户端视觉验收，因此没有扩展其他动作，也不能宣布全量兼容完成。

## 项目、备份与回退

- 修改目录：`D:\车万女仆\更多动作1.20\TLMAdditionExample-master`
- 备份：`D:\车万女仆\更多动作1.20\TLMAdditionExample-master-backup-ysm-20260905-095957`
- 备份包含当时的源码、未跟踪文件、构建配置和 `.git`；未复制可重建的 `build`、`.gradle`。
- 工作分支：`ysm-animation-compat`。没有合并、推送或修改 `master` 的提交指针。
- 原有未提交修改已保存为本分支基准提交 `49b60e9`，不是本次兼容改动。
- 回退兼容代码时，优先对后续 PoC 提交执行 `git revert`；不要使用会丢弃后续工作的 `reset --hard`。完整原始文件也保存在上述备份。
- 没有修改提供的 YSM JAR，没有替换游戏 mods 中的文件。

## 真实接入链路

```text
既有事件 / TerminalControlPacket
  → MaidAnimationData.start
  → AnimationSyncPacket
  → MaidAnimationData.clientStart
  → MaidAnimationData.activeAction / activeStart
  → YsmAnimatableMixin
  → YsmAnimationBridge.after
  → CircleDanceClip 读取现有 unknown.animation.json 的 circledance
  → 官方 YSM 每实体模型的骨骼缓冲区
  → 官方 YSM 原有渲染器
```

实际挂钩是官方 JAR 中：

```text
com.elfmcys.yesstevemodel.o0000OoOooO0oo0o0oooo0Oo
  o0OOooo0o0OO00OoOOOo0o0O(float, boolean)
  返回 OO00O0o0OooOOOo00OO00o00
```

该方法先计算动画，再返回供渲染器使用的事件。HEAD 恢复上次写入前的骨骼分量，RETURN 采样并应用本次动作。实际方法、返回描述符及骨骼 getter/setter 均用 JDK `javap` 核对过提供的 JAR。

实体访问：`OO00OOOOo0Ooo0oo0o0Oo0OO()`；当前模型：`OOOoOO000000o0o0oOooo0o0()`。模型类 `OOOO0O0O000O000000oOOO0o` 的 `O00OOOooOoooOoo0o0o0oO0O()` 提供骨骼映射。接口 `Oo0o00oOOo0OO000000O0oO0` 的实现 `OO0oo000o00O0O0oo00oO000` 直接读写每实例 float 缓冲区。

这不是把 JSON 放进 YSM 目录让它自动识别，也没有注册一套 YSM controller。桥接层只采样当前已验证为纯数字、线性关键帧的单个动作，写入官方已有的骨骼 runtime；没有新 renderer、没有独立触发条件、没有新增网络协议。

## JAR 与参考资料

提供的 JAR 元数据：`yes_steve_model` / `2.6.5-forge+mc1.20.1`。

SHA-256：`25B5E902B96F4C298690208F8B433CBC31737C23F87590354DBD86F00207BC8F`。

[ysm_epicfight_compat](https://github.com/HSZK2017/ysm_epicfight_compat) 所带 `libs/ysm-2.6.5.jar` 与提供的 JAR 哈希相同。但它的 `YSMRuntimeBridge` / `YSMPlayerAnimator` / `ScriptAnim` 服务于它自己的 Epic Fight 网格和脚本计算，并不能证明向官方原 renderer 注入动画的接口。因此这里只参考其状态隔离和调用组织，没有复制其战斗系统或网格渲染实现。

[官方 YSM](https://github.com/YesSteveModel/YesSteveModel) 当前公开代码处于重构阶段，不用其新接口代替 2.6.5 实际 JAR。[OpenYSM](https://github.com/OpenYSM/OpenYSM) 仅用于辅助理解骨骼布局、旋转单位和渲染顺序；最终混淆名称以本地 JAR 为准。

**OpenYSM 是否进入依赖：否。** YSM 本身也没有加入编译或运行时强制依赖。

## 范围与生命周期

- 当前仅 `activeAction == circledance` 时应用；动作采样使用既有 `activeStart`，不会每帧从零开始。
- 直接读取已有资源 `moreanimation:animation/unknown.animation.json`，没有复制动画数值。
- 当前动作 4 秒循环，15 条通道，包含位置与旋转。保留完整旋转圈数；旋转转换为 YSM 的弧度和轴向。
- 骨骼优先同名匹配；仅当缺少 `MRoot` 且有 `MAllBody` 时使用该别名，依据官方内置默认模型的结构。其他缺失骨骼跳过。
- 没有自动推断任意自定义骨架。缺少关键骨骼时，舞蹈可能不完整，必须实测模型。
- 保存原始骨骼分量的表按 animatable 对象隔离，使用弱键；不会将动作状态放在共享模型资源里。
- 动作结束或切换到其他动作时，下一次姿态计算先恢复原值，再交回 YSM。其他动作目前不做兼容。
- 资源重载清除动画缓存。未知格式、Molang、复杂关键帧会拒绝并记录日志，不静默近似。
- 只启用已核对版本；反射失败会记录错误并停止兼容写入。可选 `@Pseudo` Mixin 在没有目标类时跳过。

## 文件清单

修改：

1. `src/main/resources/mixins.moreanimation.json`：增加可选客户端 Mixin。

新增：

1. `src/main/java/com/github/JumDa5he/moreanimation/compat/ysm/CircleDanceClip.java`
2. `src/main/java/com/github/JumDa5he/moreanimation/compat/ysm/YsmAnimationBridge.java`
3. `src/main/java/com/github/JumDa5he/moreanimation/mixin/YsmAnimatableMixin.java`
4. `scripts/ysm/CircleDanceClipCheck.java`
5. `scripts/ysm/OfficialBoneCheck.java`
6. `docs/YSM-COMPAT-POC.md`：本报告。

没有修改现有 MaidAnimationData、AnimationSyncPacket、Gecko 动画实现或 Gradle 依赖。

## 验证结果

- JDK 17，实际执行 `gradlew.bat build --offline`：**BUILD SUCCESSFUL**，35 秒。
- Gradle 没有现成测试源，`test NO-SOURCE`；不能把这一项当作运行时测试。
- 独立采样检查：读取项目真实动画，验证插值、循环、常量通道、完整转圈、拒绝未支持的 Molang，全部通过。
- 官方骨骼检查：在哈希相同的官方 JAR 上实例化真实骨骼类，验证九个缓冲区读写分量、恢复原值和两个实例的缓冲区隔离，全部通过。
- 以上检查**不覆盖** Forge 启动、Mixin 实际应用、游戏渲染、多客户端同步或视觉质量。

## 唯一推荐的下一步：游戏内 circledance 门槛验收

使用备份后的测试实例安装本次 MoreAnimation 验证 JAR，保留官方 YSM 2.6.5 和 TLM，不要同时放入两个 MoreAnimation JAR。

1. 准备普通模型女仆和两只使用同一 YSM 模型的女仆。
2. 用现有动作控制终端，对其中一只播放站立动作 `circledance`；该入口通过既有 `MaidAnimationData.start`。
3. 确认只有目标女仆跳舞，普通模型仍正常，另一只 YSM 女仆不跟随跳舞。
4. 确认动作连续播放、旋转不反复重置；停止、切换动作和自然结束后恢复 YSM 的正常姿态。
5. 切换 YSM 模型、重载资源、离开重进世界，再测试；另测不装 YSM 时能正常启动和播放普通动作。
6. 日志出现 `YSM circledance PoC applied ...` 仅表明写入发生，不能代替肉眼验收。若出现 `Disabling YSM circledance PoC`，保留堆栈并停止扩展。

没有实际进入 Minecraft 进行上述验收，因此单动作门槛状态是“待验证”，不是“成功”或“已证明失败”。在门槛通过前，完整兼容动作清单为空；当前可供测试的只有 `circledance`。
