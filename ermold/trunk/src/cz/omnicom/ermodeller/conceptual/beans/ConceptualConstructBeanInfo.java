package cz.omnicom.ermodeller.conceptual.beans;

/**
 * The bean information class for cz.omnicom.ermodeller.conceptual.ConceptualConstruct.
 */
public class ConceptualConstructBeanInfo extends java.beans.SimpleBeanInfo {
    /**
     * Gets the atributes property descriptor.
     *
     * @return java.beans.PropertyDescriptor
     */
    public java.beans.PropertyDescriptor atributesPropertyDescriptor() {
        java.beans.PropertyDescriptor aDescriptor = null;
        try {
            try {
                /* Using methods via getMethod is the faster way to create the atributes property descriptor. */
                java.lang.reflect.Method aGetMethod;
                try {
                    /* Attempt to find the method using getMethod with parameter types. */
                    java.lang.Class aGetMethodParameterTypes[] = {};
                    aGetMethod = getBeanClass().getMethod("getAtributes", aGetMethodParameterTypes);
                } catch (Throwable exception) {
                    /* Since getMethod failed, call findMethod. */
                    handleException(exception);
                    aGetMethod = findMethod(getBeanClass(), "getAtributes", 0);
                }
                java.lang.reflect.Method aSetMethod = null;
                aDescriptor = new java.beans.PropertyDescriptor("atributes"
                        , aGetMethod, aSetMethod);
            } catch (Throwable exception) {
                /* Since we failed using methods, try creating a default property descriptor. */
                handleException(exception);
                aDescriptor = new java.beans.PropertyDescriptor("atributes"
                        , getBeanClass());
            }
            aDescriptor.setBound(true);
            /* aDescriptor.setConstrained(false); */
            /* aDescriptor.setDisplayName("atributes"); */
            /* aDescriptor.setShortDescription("atributes"); */
            /* aDescriptor.setExpert(false); */
            aDescriptor.setHidden(true);
            /* aDescriptor.setValue("preferred", new Boolean(false)); */
            aDescriptor.setValue("ivjDesignTimeProperty", Boolean.FALSE);
        } catch (Throwable exception) {
            handleException(exception);
        }
        return aDescriptor;
    }

    /**
     * Gets the cardinalities property descriptor.
     *
     * @return java.beans.PropertyDescriptor
     */
    public java.beans.PropertyDescriptor cardinalitiesPropertyDescriptor() {
        java.beans.PropertyDescriptor aDescriptor = null;
        try {
            try {
                /* Using methods via getMethod is the faster way to create the cardinalities property descriptor. */
                java.lang.reflect.Method aGetMethod;
                try {
                    /* Attempt to find the method using getMethod with parameter types. */
                    java.lang.Class aGetMethodParameterTypes[] = {};
                    aGetMethod = getBeanClass().getMethod("getCardinalities", aGetMethodParameterTypes);
                } catch (Throwable exception) {
                    /* Since getMethod failed, call findMethod. */
                    handleException(exception);
                    aGetMethod = findMethod(getBeanClass(), "getCardinalities", 0);
                }
                java.lang.reflect.Method aSetMethod = null;
                aDescriptor = new java.beans.PropertyDescriptor("cardinalities"
                        , aGetMethod, aSetMethod);
            } catch (Throwable exception) {
                /* Since we failed using methods, try creating a default property descriptor. */
                handleException(exception);
                aDescriptor = new java.beans.PropertyDescriptor("cardinalities"
                        , getBeanClass());
            }
            aDescriptor.setBound(true);
            /* aDescriptor.setConstrained(false); */
            /* aDescriptor.setDisplayName("cardinalities"); */
            /* aDescriptor.setShortDescription("cardinalities"); */
            /* aDescriptor.setExpert(false); */
            aDescriptor.setHidden(true);
            /* aDescriptor.setValue("preferred", new Boolean(false)); */
            aDescriptor.setValue("ivjDesignTimeProperty", Boolean.FALSE);
        } catch (Throwable exception) {
            handleException(exception);
        }
        return aDescriptor;
    }

    /**
     * Find the method by comparing (name & parameter size) against the methods in the class.
     *
     * @param aClass         java.lang.Class
     * @param methodName     java.lang.String
     * @param parameterCount int
     * @return java.lang.reflect.Method
     */
    public static java.lang.reflect.Method findMethod(java.lang.Class aClass, java.lang.String methodName, int parameterCount) {
        try {
            /* Since this method attempts to find a method by getting all methods from the class,
       this method should only be called if getMethod cannot find the method. */
            java.lang.reflect.Method methods[] = aClass.getMethods();
            for (int index = 0; index < methods.length; index++) {
                java.lang.reflect.Method method = methods[index];
                if ((method.getParameterTypes().length == parameterCount) && (method.getName().equals(methodName))) {
                    return method;
                }
            }
        } catch (java.lang.Throwable exception) {
            return null;
        }
        return null;
    }

    /**
     * Returns the BeanInfo of the superclass of this bean to inherit its features.
     *
     * @return java.beans.BeanInfo[]
     */
    public java.beans.BeanInfo[] getAdditionalBeanInfo() {
        java.lang.Class superClass;
        java.beans.BeanInfo superBeanInfo = null;

        try {
            superClass = getBeanDescriptor().getBeanClass().getSuperclass();
        } catch (java.lang.Throwable exception) {
            return null;
        }

        try {
            superBeanInfo = java.beans.Introspector.getBeanInfo(superClass);
        } catch (java.beans.IntrospectionException ie) {
        }

        if (superBeanInfo != null) {
            java.beans.BeanInfo[] ret = new java.beans.BeanInfo[1];
            ret[0] = superBeanInfo;
            return ret;
        }
        return null;
    }

    /**
     * Gets the bean class.
     *
     * @return java.lang.Class
     */
    public static java.lang.Class getBeanClass() {
        return ConceptualConstruct.class;
    }

    /**
     * Gets the bean class name.
     *
     * @return java.lang.String
     */
    public static java.lang.String getBeanClassName() {
        return ConceptualConstruct.class.getName();
    }

    public java.beans.BeanDescriptor getBeanDescriptor() {
        java.beans.BeanDescriptor aDescriptor = null;
        try {
            /* Create and return the ConceptualConstructBeanInfo bean descriptor. */
            aDescriptor = new java.beans.BeanDescriptor(ConceptualConstruct.class);
            /* aDescriptor.setExpert(false); */
            /* aDescriptor.setHidden(false); */
            /* aDescriptor.setValue("hidden-state", Boolean.FALSE); */
        } catch (Throwable exception) {
        }
        return aDescriptor;
    }

    /**
     * Return the event set descriptors for this bean.
     *
     * @return java.beans.EventSetDescriptor[]
     */
    public java.beans.EventSetDescriptor[] getEventSetDescriptors() {
        try {
            java.beans.EventSetDescriptor aDescriptorList[] = {
            };
            return aDescriptorList;
        } catch (Throwable exception) {
            handleException(exception);
        }
        return null;
    }

    /**
     * Return the method descriptors for this bean.
     *
     * @return java.beans.MethodDescriptor[]
     */
    public java.beans.MethodDescriptor[] getMethodDescriptors() {
        try {
            java.beans.MethodDescriptor aDescriptorList[] = {
            };
            return aDescriptorList;
        } catch (Throwable exception) {
            handleException(exception);
        }
        return null;
    }

    /**
     * Return the property descriptors for this bean.
     *
     * @return java.beans.PropertyDescriptor[]
     */
    public java.beans.PropertyDescriptor[] getPropertyDescriptors() {
        try {
            java.beans.PropertyDescriptor aDescriptorList[] = {
                    atributesPropertyDescriptor()
                    , cardinalitiesPropertyDescriptor()
            };
            return aDescriptorList;
        } catch (Throwable exception) {
            handleException(exception);
        }
        return null;
    }

    /**
     * Called whenever the bean information class throws an exception.
     *
     * @param exception java.lang.Throwable
     */
    private void handleException(java.lang.Throwable exception) {

        /* Uncomment the following lines to print uncaught exceptions to stdout */
        // System.out.println("--------- UNCAUGHT EXCEPTION ---------");
        // exception.printStackTrace(System.out);
    }
}
