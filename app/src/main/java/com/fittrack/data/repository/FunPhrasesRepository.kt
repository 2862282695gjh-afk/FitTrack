package com.fittrack.data.repository

import kotlin.random.Random

/**
 * 俏皮话库 - 类似百邻国的激励文案
 */
object FunPhrasesRepository {

    /**
     * 激励文案列表
     */
    private val motivationalPhrases = listOf(
        "今天不流汗，明天流眼泪",
        "汗水是脂肪的眼泪，让它尽情流吧",
        "每一次训练，都是未来的自己感谢现在的你",
        "不想认命，那就拼命",
        "你的身体是宇宙的一部分，好好照顾它",
        "汗水不会说谎，坚持会有回报",
        "今天的你比昨天更强一点点",
        "健身是最好的整容，免费的",
        "没有什么比挥汗如雨更痛快",
        "自律给我自由，坚持给我力量",
        "你流的每一滴汗，都在雕刻更好的自己",
        "别找借口，去找你想要的身材",
        "今天多吃一口，明天多跑一里",
        "健身不是为了让别人看，而是为了自己爽",
        "肌肉不会骗你，你给它多少努力，它就给你多少回报",
        "别人休息的时候，你在变强",
        "懒惰是最大的敌人，战胜它你就是英雄",
        "每一组都是挑战，每一滴都是荣耀",
        "健身房里没有奇迹，只有汗水和坚持",
        "最好的投资是投资自己，现在就开始",
        "你流过的汗，会变成你的肌肉",
        "别等到身材走样才后悔，现在开始还不晚",
        "健身是一生的修行，今天只是开始",
        "你的潜力远超你的想象，去挑战它",
        "不要让明天的你讨厌今天的自己",
        "坚持不是因为你看到了希望，而是坚持才能看到希望",
        "每一次力竭都是成长的开始",
        "你的身体是你的神庙，好好维护它",
        "今天的痛苦是明天的勋章",
        "没有人能替你训练，除了你自己",
        "当你觉得累的时候，就是你变得更强的时候",
        "别羡慕别人的身材，你也可以有",
        "健身路上没有捷径，只有一步一个脚印",
        "每一滴汗水都在为未来投资",
        "你流的每一滴汗，都让你离梦想更近一步",
        "今天的训练，明天的好身材",
        "别给自己找理由，给自己的身体找借口",
        "坚持很难，放弃更难，你选哪一个",
        "健身房是你和自己的战场，去战斗吧",
        "每一次训练都是对自己的承诺"
    )

    /**
     * 轻松幽默文案（适合休息日）
     */
    private val restDayPhrases = listOf(
        "休息是为了更好地出发",
        "今天休息日，肌肉在偷偷生长中",
        "给身体放个假，明天满血复活",
        "休息也是训练的一部分",
        "肌肉恢复中，请勿打扰",
        "今天没有训练任务，心情不错吧",
        "休息日也是好日子，好好享受",
        "肌肉在偷偷感谢今天的休息",
        "今天不撸铁，明天更有力",
        "休息是最好的恢复剂"
    )

    /**
     * 逾期提醒文案
     */
    private val overduePhrases = listOf(
        "哎呀，训练逾期了～赶紧补上吧",
        "你的肌肉在等不及了，快去训练",
        "别让它等太久，它会伤心的",
        "今天训练，明天更强",
        "逾期不可怕，可怕的是放弃",
        "快去完成训练，别让汗水白流",
        "训练还在等你，别让它失望",
        "每一刻的拖延，都是对梦想的辜负",
        "还等什么，现在就开始",
        "逾期训练，加倍回报"
    )

    /**
     * 获取随机激励文案
     */
    fun getRandomMotivationalPhrase(): String {
        return motivationalPhrases[Random.nextInt(motivationalPhrases.size)]
    }

    /**
     * 获取随机休息日文案
     */
    fun getRandomRestDayPhrase(): String {
        return restDayPhrases[Random.nextInt(restDayPhrases.size)]
    }

    /**
     * 获取随机逾期提醒文案
     */
    fun getRandomOverduePhrase(): String {
        return overduePhrases[Random.nextInt(overduePhrases.size)]
    }

    /**
     * 根据状态获取合适的俏皮话
     */
    fun getPhraseForStatus(status: String): String {
        return when (status) {
            "rest", "idle" -> getRandomRestDayPhrase()
            "overdue" -> getRandomOverduePhrase()
            "pending" -> getRandomMotivationalPhrase()
            else -> getRandomMotivationalPhrase()
        }
    }
}
