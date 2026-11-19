# StarTrails Release 混淆规则
#
# Room / Hilt / WorkManager / Compose 的 AAR 都自带 consumer 规则，
# 这里只补充本项目依赖“运行时名称”的部分。

# 注解与泛型签名会被 Hilt、Room、WorkManager 在运行时读取。
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# 任务状态、错误码、叠加模式等枚举会以 name() 写入 Room 与 WorkInfo，
# 一旦被重命名，升级后的旧数据将无法解析。
-keepnames enum com.startrails.core.model.** { *; }

# @HiltWorker 由 HiltWorkerFactory 通过生成的工厂创建，保留类名便于日志与崩溃定位。
-keepnames class com.startrails.core.domain.StackWorker

# WorkManager 在恢复任务时按类名反序列化 Worker。
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}
