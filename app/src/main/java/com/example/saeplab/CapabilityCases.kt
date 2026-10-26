package com.example.saeplab

data class CapabilityCase(val id: String, val title: String, val description: String, val instruction: String)

object CapabilityCases {
    val all = listOf(
        CapabilityCase("form", "表单选择", "下拉菜单、勾选、开关、日期与保存。",
            "打开 Agent 实验室的功能能力，进入表单选择。把分类选为工作，勾选需要提醒，打开置顶开关，把日期选为2026年10月20日，然后点击保存表单。完成后停止。"),
        CapabilityCase("list", "列表查找", "滚动查找与读取屏幕上的校验码。",
            "打开 Agent 实验室的功能能力，进入列表查找。向下滚动找到项目12，记住该行显示的六位校验码，点击选择项目12，在弹窗中填入刚才的校验码并确认。完成后停止。"),
        CapabilityCase("long_press", "长按菜单", "长按测试卡片，并选择菜单项目。",
            "打开 Agent 实验室的功能能力，进入长按菜单。长按测试卡片，在出现的菜单中选择收藏。完成后停止。"),
        CapabilityCase("drag", "拖拽移动", "按住方块，拖到目标区后松手。",
            "打开 Agent 实验室的功能能力，进入拖拽移动。长按左边蓝色方块，保持按住并拖到右边目标区，再松手。完成后停止；如果无法执行，请直接说明。"),
        CapabilityCase("file", "文件选择", "通过系统文件选择器导入，再导出文件。",
            "打开 Agent 实验室的功能能力，进入文件选择。点击选择测试文件，在下载目录的SAEP-Lab文件夹选择AgentLab-fixture.txt。回到App后，点击导出结果文件。完成后停止。"),
        CapabilityCase("record", "记录编辑", "编辑示例记录，删除可删除的临时记录。",
            "打开 Agent 实验室的功能能力，进入记录编辑。点击编辑示例记录，把标题改为能力已确认并保存。然后删除标记为可删除的临时记录，在确认弹窗中确认删除。这些都是专门用于测试的本地样例数据。完成后停止。")
    )
}
