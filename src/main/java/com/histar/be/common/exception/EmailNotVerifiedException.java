package com.histar.be.common.exception;

public class EmailNotVerifiedException extends BaseException {

    public EmailNotVerifiedException() {
        super(ErrorCode.EMAIL_NOT_VERIFIED, "Vui lòng xác thực email trước khi thanh toán. Kiểm tra hộp thư hoặc gửi lại email xác thực.");
    }
}
