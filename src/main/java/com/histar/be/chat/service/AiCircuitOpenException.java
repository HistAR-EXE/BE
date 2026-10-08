package com.histar.be.chat.service;

import com.histar.be.common.exception.BusinessRuleException;

public class AiCircuitOpenException extends BusinessRuleException {

    public AiCircuitOpenException(String dependency) {
        super("Dịch vụ AI " + dependency + " đang tạm nghỉ sau các lỗi liên tiếp. Vui lòng thử lại sau ít phút.");
    }
}
