package com.sm.orderingagent.ui.customer

import com.sm.orderingagent.domain.MenuItem
import com.sm.orderingagent.domain.Order

/**
 * 纯顾客屏的核心场景状态枚举
 */
sealed interface CustomerStage {
    /** 待机与菜单全览 */
    object Menu : CustomerStage

    /** 购物车核对与明细审查 */
    object CartReview : CustomerStage

    /** 规格或多候选菜品澄清 */
    data class Clarify(
        val candidates: List<MenuItem>,
        val queryText: String = ""
    ) : CustomerStage

    /** 订单已提交，等待出餐/取餐指引 */
    data class Submitted(
        val order: Order
    ) : CustomerStage

    /** 呼叫人工服务或发生无法恢复的异常接管 */
    data class Handoff(
        val reason: String = "正在为您呼叫服务员，请稍候并保持在车道内。"
    ) : CustomerStage
}
