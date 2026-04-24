package com.weifurry.spotfurry.presentation.player

import com.weifurry.spotfurry.presentation.model.Playlist
import com.weifurry.spotfurry.presentation.model.Track

internal fun previewPlaylists(): List<Playlist> =
    listOf(
        Playlist(
            id = "night-drive",
            name = "夜行驾驶",
            subtitle = "适合夜晚通勤的温暖合成器旋律",
            tracks =
                listOf(
                    Track("t1", "余晖电路", "新星脉冲", 214),
                    Track("t2", "月光出口", "丝绒信号", 201),
                    Track("t3", "静电心跳", "紫苑小径", 226),
                    Track("t4", "玻璃公路", "天穹回声", 192)
                )
        ),
        Playlist(
            id = "focus-loop",
            name = "专注循环",
            subtitle = "适合短时专注的平静播放队列",
            tracks =
                listOf(
                    Track("t5", "柔和轨道", "灰烬山谷", 178),
                    Track("t6", "静默二进制", "北方气流", 242),
                    Track("t7", "流明脚本", "光环漂流", 206),
                    Track("t8", "洁净房间", "高空绽放", 187)
                )
        ),
        Playlist(
            id = "run-mode",
            name = "跑步模式",
            subtitle = "适合运动节奏的高能旋律",
            tracks =
                listOf(
                    Track("t9", "火花轨迹", "风筝星图", 181),
                    Track("t10", "速度绽放", "六月街机", 195),
                    Track("t11", "脉冲并行", "回声港湾", 204),
                    Track("t12", "霓虹冲刺", "铆钉青春", 176)
                )
        )
    )
