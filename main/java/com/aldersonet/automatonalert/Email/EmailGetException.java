package com.aldersonet.automatonalert.Email;

public class EmailGetException extends Exception {

    /**
	 * 
	 */
	private static final long serialVersionUID = -3533685765264855386L;
	boolean mFatal = false;

    public EmailGetException(String detail) {
        super(detail);
    }

    public EmailGetException(String detail, boolean fatal) {
        super(detail);
        mFatal = fatal;
    }

    public EmailGetException(String detail, Throwable throwable) {
        super(detail, throwable);
    }

    public EmailGetException(String detail, boolean fatal, Throwable throwable) {
        super(detail, throwable);
        mFatal = fatal;
    }

    public boolean isFatalException() {
        return mFatal;
    }

    public void setFatalException(boolean fatal) {
        this.mFatal = fatal;
    }


}