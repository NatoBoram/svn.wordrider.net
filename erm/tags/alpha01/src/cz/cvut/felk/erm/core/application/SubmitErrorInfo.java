package cz.cvut.felk.erm.core.application;

import cz.cvut.felk.erm.core.AppPrefs;
import cz.cvut.felk.erm.core.Consts;
import cz.cvut.felk.erm.core.UserProp;
import cz.cvut.felk.erm.utilities.Utils;
import org.jdesktop.beans.AbstractBean;
import org.jdesktop.swingx.error.ErrorInfo;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Info pro odeslani na server
 * @author Ladislav Vitasek
 */
public class SubmitErrorInfo extends AbstractBean {
    private String name = "";
    private String email = "";
    private String comment = "";
    private ErrorInfo errorInfo;

    public SubmitErrorInfo(ErrorInfo errorInfo) {
        super();
        this.errorInfo = errorInfo;
        this.setName(AppPrefs.getProperty(UserProp.SUBMIT_ERROR_NAME));
        this.setEmail(AppPrefs.getProperty(UserProp.SUBMIT_ERROR_EMAIL));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        String oldValue = getName();
        this.name = name;
        firePropertyChange("name", oldValue, name);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        String oldValue = getEmail();
        this.email = email;
        firePropertyChange("email", oldValue, email);
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        String oldValue = getComment();
        this.comment = comment;
        firePropertyChange("comment", oldValue, comment);
    }

    public ErrorInfo getErrorInfo() {
        return errorInfo;
    }

    private static String getUserInfo() {
        final StringBuilder builder = new StringBuilder();
        final String unknown = "Unknown";
        builder.append("java.version=").append(System.getProperty("java.version", unknown)).append('\n');
        builder.append("os.name=").append(System.getProperty("os.name", unknown)).append('\n');
        builder.append("user.language=").append(System.getProperty("user.language", unknown)).append('\n');
        builder.append("user.name=").append(System.getProperty("user.name", unknown)).append('\n');
        builder.append("user.dir=").append(System.getProperty("user.dir", unknown)).append('\n');
        builder.append("user.country=").append(System.getProperty("user.country", unknown)).append('\n');
        return builder.toString();
    }


    public String toURLPostData() {
        AppPrefs.storeProperty(UserProp.SUBMIT_ERROR_EMAIL, getEmail());
        AppPrefs.storeProperty(UserProp.SUBMIT_ERROR_NAME, getName());

        final StringBuilder builder = new StringBuilder();
        Utils.addParam(builder, "product", Consts.PRODUCT);
        Utils.addParam(builder, "version", Consts.VERSION);
        Utils.addParam(builder, "name", getName());
        Utils.addParam(builder, "comment", getComment());
        Utils.addParam(builder, "email", getEmail());
        Utils.addParam(builder, "userinfo", getUserInfo());
        getErrorInfo().getErrorException().printStackTrace();
        final StringWriter sw = new StringWriter();
        getErrorInfo().getErrorException().printStackTrace(new PrintWriter(sw));
        Utils.addParam(builder, "exception", sw.toString());
        return builder.toString();
    }

}
