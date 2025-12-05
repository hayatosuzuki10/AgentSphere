package primula.api.core.network.message;

import java.io.Serializable;

// okubo 
// 2013/11/13
// ModuleAgent専用コンテナ


public class VariableTypeContentContainer extends AbstractContentContainer {

    private Serializable content;
    Class<?> contentType;

    public VariableTypeContentContainer(Serializable content) {
    	contentType = content.getClass();
        this.content = content;
    }

    public Serializable getContent() {
        return content;
    }
    
    public Class<?> getContentType() {
    	return contentType;
    }
    
    public String getContentTypeName() {
    	return contentType.getSimpleName();
    }

}
