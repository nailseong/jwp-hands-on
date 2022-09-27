package nextstep.study.di.stage3.context;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 스프링의 BeanFactory, ApplicationContext에 해당되는 클래스
 */
class DIContainer {

    private static final Logger log = LoggerFactory.getLogger(DIContainer.class);

    private final Set<Object> beans;

    public DIContainer(final Set<Class<?>> classes) {
        this.beans = toBeans(classes);
    }

    private Set<Object> toBeans(final Set<Class<?>> classes) {
        final Set<Object> beanInstances = classes.stream()
                .map(this::instantiateBean)
                .collect(Collectors.toSet());
        return toInjectedBeans(beanInstances);
    }

    private Object instantiateBean(final Class<?> clazz) {
        try {
            final Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (final Exception e) {
            log.warn("빈 생성 실패 {}", e);
            return null;
        }
    }

    private Set<Object> toInjectedBeans(final Set<Object> beanInstances) {
        for (final Object beanInstance : beanInstances) {
            log.info("Inject to {}", beanInstance.getClass());
            final Field[] fields = beanInstance.getClass()
                    .getDeclaredFields();
            for (final Field field : fields) {
                getInjectableBean(beanInstances, field)
                        .ifPresent(it -> injectBeanToField(beanInstance, field, it));
            }
            beanInstances.add(beanInstance);
        }
        return beanInstances;
    }

    private Optional<Object> getInjectableBean(final Set<Object> beanInstances, final Field field) {
        return beanInstances.stream()
                .filter(it -> field.getType().isInstance(it))
                .findFirst();
    }

    private void injectBeanToField(final Object injectTarget, final Field field, final Object bean) {
        try {
            field.setAccessible(true);
            field.set(injectTarget, bean);
        } catch (final Exception e) {
            log.warn("빈 의존성 주입 실패 {}", e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(final Class<T> aClass) {
        return (T) beans.stream()
                .filter(aClass::isInstance)
                .findFirst()
                .orElseThrow();
    }
}
