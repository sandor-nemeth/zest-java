package org.qi4j.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import org.qi4j.api.DependencyKey;
import org.qi4j.api.annotation.AppliesTo;
import org.qi4j.api.annotation.DependencyScope;
import org.qi4j.api.annotation.Name;
import org.qi4j.api.annotation.Optional;
import org.qi4j.api.annotation.Property;
import org.qi4j.api.model.ConstructorDependency;
import org.qi4j.api.model.FieldDependency;
import org.qi4j.api.model.InvalidCompositeException;
import org.qi4j.api.model.MethodDependency;
import org.qi4j.api.model.ParameterDependency;

/**
 * TODO
 */
public abstract class FragmentModelBuilder
{
    protected void getConstructorDependencies( Class mixinClass, Class compositeType, List<ConstructorDependency> dependentConstructors )
    {
        Constructor[] constructors = mixinClass.getConstructors();
        for( Constructor constructor : constructors )
        {
            Type[] parameterTypes = constructor.getGenericParameterTypes();
            Annotation[][] parameterAnnotations = constructor.getParameterAnnotations();
            List<ParameterDependency> constructorParameters = new ArrayList<ParameterDependency>();
            getParameterDependencies( mixinClass, compositeType, parameterTypes, parameterAnnotations, constructorParameters );

            ConstructorDependency dependency = new ConstructorDependency( constructor, constructorParameters );
            dependentConstructors.add( dependency );
        }
    }

    protected void getMethodDependencies( Class mixinClass, Class compositeType, List<MethodDependency> dependentMethods )
    {
        Method[] methods = mixinClass.getMethods();
        for( Method method : methods )
        {
            Type[] parameterTypes = method.getGenericParameterTypes();
            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            if( hasDependencyAnnotation( parameterAnnotations ) )
            {
                List<ParameterDependency> methodParameters = new ArrayList<ParameterDependency>();
                getParameterDependencies( mixinClass, compositeType, parameterTypes, parameterAnnotations, methodParameters );

                MethodDependency dependency = new MethodDependency( method, methodParameters );

                dependentMethods.add( dependency );
            }
        }
    }

    protected void getFieldDependencies( Class fragmentClass, Class compositeType, List<FieldDependency> dependentFields )
    {
        Field[] fields = fragmentClass.getDeclaredFields();
        for( Field field : fields )
        {
            // Find annotation that is a DependencyAnnotation
            Annotation[] annotations = field.getAnnotations();
            Annotation annotation = getDependencyAnnotation( annotations );
            if( annotation != null )
            {
                field.setAccessible( true );
                String name = null;
                boolean optional = false;

                Method optionalMethod = getAnnotationMethod( Optional.class, annotation.annotationType() );
                if( optionalMethod != null )
                {
                    try
                    {
                        optional = Boolean.class.cast( optionalMethod.invoke( annotation ) );
                    }
                    catch( Exception e )
                    {
                        throw new InvalidCompositeException( "Could not get optional flag from annotation", fragmentClass );
                    }
                }

                Method nameMethod = getAnnotationMethod( Name.class, annotation.annotationType() );
                if( nameMethod != null )
                {
                    String specifiedName;
                    try
                    {
                        specifiedName = String.class.cast( nameMethod.invoke( annotation ) );
                    }
                    catch( Exception e )
                    {
                        throw new InvalidCompositeException( "Could not get name flag from annotation", fragmentClass );
                    }
                    if( specifiedName.equals( "" ) )
                    {
                        name = specifiedName;
                    }
                }

                DependencyKey key = new DependencyKey( annotation.annotationType(), field.getType(), name, fragmentClass, compositeType );

                FieldDependency dependency = new FieldDependency( key, optional, field );

                dependentFields.add( dependency );
            }
        }

        // Add fields in superclass
        Class<?> parent = fragmentClass.getSuperclass();
        if( parent != null && parent != Object.class )
        {
            getFieldDependencies( parent, compositeType, dependentFields );
        }
    }

    protected Method getAnnotationMethod( Class<? extends Annotation> anAnnotationClass, Class<? extends Annotation> aClass )
    {
        Method[] methods = aClass.getMethods();
        for( Method method : methods )
        {
            if( method.getAnnotation( anAnnotationClass ) != null )
            {
                return method;
            }
        }
        return null;
    }

    protected boolean isDependencyAnnotation( Annotation annotation )
    {
        return annotation.annotationType().getAnnotation( DependencyScope.class ) != null;
    }

    protected Class getAppliesTo( Class<? extends Object> aModifierClass )
    {
        AppliesTo appliesTo = aModifierClass.getAnnotation( AppliesTo.class );
        if( appliesTo != null )
        {
            return appliesTo.value();
        }

        Class<?> parent = aModifierClass.getSuperclass();
        if( parent != null && parent != Object.class )
        {
            return getAppliesTo( parent );
        }
        else
        {
            return null;
        }
    }

    private boolean hasDependencyAnnotation( Annotation[][] parameterAnnotation )
    {
        for( Annotation[] annotations : parameterAnnotation )
        {
            for( Annotation annotation : annotations )
            {
                if( isDependencyAnnotation( annotation ) )
                {
                    return true;
                }
            }
        }
        return false;
    }

    private void getParameterDependencies( Class mixinClass, Class compositeType, Type[] parameterTypes, Annotation[][] parameterAnnotations, List<ParameterDependency> parameterDependencies )
    {
        int i = 0;
        for( Type parameterType : parameterTypes )
        {
            // Find annotation that is a DependencyAnnotation
            Annotation annotation = getDependencyAnnotation( parameterAnnotations[ i ] );
            ParameterDependency dependency = null;
            if( annotation != null )
            {
                String name = null;
                boolean optional = isOptional( annotation );

                Method nameMethod = getAnnotationMethod( Name.class, annotation.getClass() );
                if( nameMethod != null )
                {
                    String specifiedName;
                    try
                    {
                        specifiedName = String.class.cast( nameMethod.invoke( annotation ) );
                    }
                    catch( Exception e )
                    {
                        throw new InvalidCompositeException( "Could not get optional flag from annotation", mixinClass );
                    }
                    if( specifiedName.equals( "" ) )
                    {
                        name = specifiedName;
                    }
                }

                DependencyKey key = new DependencyKey( annotation.annotationType(), parameterType, name, mixinClass, compositeType );
                dependency = new ParameterDependency( key, optional, name );
                parameterDependencies.add( dependency );
            }
            else
            {
                // Fake a Property annotation
                DependencyKey key = new DependencyKey( Property.class, parameterType, null, mixinClass, compositeType );
                dependency = new ParameterDependency( key, false, null );
                parameterDependencies.add( dependency );
            }

            i++;
        }
    }

    private boolean isOptional( Annotation annotation )
    {
        Method optionalMethod = getAnnotationMethod( Optional.class, annotation.getClass() );
        if( optionalMethod != null )
        {
            try
            {
                return Boolean.class.cast( optionalMethod.invoke( annotation ) );
            }
            catch( Exception e )
            {
                throw new InvalidCompositeException( "Could not get optional flag from annotation", annotation.getClass() );
            }
        }
        return false;
    }

    private Annotation getDependencyAnnotation( Annotation[] parameterAnnotation )
    {
        for( Annotation annotation : parameterAnnotation )
        {
            if( isDependencyAnnotation( annotation ) )
            {
                return annotation;
            }
        }
        return null;
    }

}