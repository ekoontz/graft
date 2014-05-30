package org.apache.giraph.debugger.mock;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.giraph.debugger.mock.ComputationComputeTestGenerator.Config;
import org.apache.giraph.debugger.utils.CommonVertexMasterContextWrapper;
import org.apache.giraph.debugger.utils.GiraphVertexScenarioWrapper.VertexScenarioClassesWrapper;
import org.apache.giraph.utils.WritableUtils;
import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.ByteWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.velocity.VelocityContext;

public abstract class TestGenerator extends VelocityBasedGenerator {

  @SuppressWarnings("rawtypes")
  private Set<Class> complexWritables = new HashSet<>();
  
  @SuppressWarnings("rawtypes")
  public Set<Class> getComplexWritableList() {
    return complexWritables;
  }

  protected void resetComplexWritableList() {
    this.complexWritables.clear();
  }

  protected class ContextBuilder {

    protected VelocityContext context;

    public ContextBuilder() {
      context = new VelocityContext();
      addHelper();
      addWritableReadFromString();
    }

    public VelocityContext getContext() {
      return context;
    }

    private void addHelper() {
      context.put("helper", new FormatHelper());
    }
    
    private void addWritableReadFromString() {
      context.put("complexWritables", complexWritables);
    }

    public void addPackage(String testPackage) {
      context.put("package", testPackage);
    }

    @SuppressWarnings("rawtypes")
    public void addClassUnderTest(Class classUnderTest) {
      context.put("classUnderTestName", classUnderTest.getSimpleName());
    }

    public void addClassName(String className) {
      if (className == null) {
        context.put("className", context.get("classUnderTestName") + "Test");
      } else {
        context.put("className", className);
      }
    }

    @SuppressWarnings("rawtypes")
    public void addTestClassInfo(String testPackage, Class classUnderTest, String className) {
      addPackage(testPackage);
      addClassUnderTest(classUnderTest);
      addClassName(className);
    }

    @SuppressWarnings("rawtypes")
    public void addVertexScenarioClassesWrapper(
        VertexScenarioClassesWrapper vertexScenarioClassesWrapper) {
      HashSet<Class> usedTypes = new LinkedHashSet<>(6);
      usedTypes.add(vertexScenarioClassesWrapper.getClassUnderTest());
      usedTypes.add(vertexScenarioClassesWrapper.getVertexIdClass());
      usedTypes.add(vertexScenarioClassesWrapper.getVertexValueClass());
      usedTypes.add(vertexScenarioClassesWrapper.getEdgeValueClass());
      usedTypes.add(vertexScenarioClassesWrapper.getIncomingMessageClass());
      usedTypes.add(vertexScenarioClassesWrapper.getOutgoingMessageClass());
      context.put("usedTypes", usedTypes);
    }

    public void addCommonMasterVertexContext(
        CommonVertexMasterContextWrapper commonVertexMasterContextWrapper) {
      context.put("superstepNo", commonVertexMasterContextWrapper.getSuperstepNoWrapper());
      context.put("nVertices", commonVertexMasterContextWrapper.getTotalNumVerticesWrapper());
      context.put("nEdges", commonVertexMasterContextWrapper.getTotalNumEdgesWrapper());

      context.put("aggregators", commonVertexMasterContextWrapper.getPreviousAggregatedValues());

      List<Config> configs = new ArrayList<>();
      if (commonVertexMasterContextWrapper.getConfig() != null) {
        for (Iterator<Entry<String, String>> configIter =
            commonVertexMasterContextWrapper.getConfig().iterator(); configIter.hasNext();) {
          Entry<String, String> entry = configIter.next();
          configs.add(new Config(entry.getKey(), entry.getValue()));
        }
      }
      context.put("configs", configs);
    }
  }

  public class FormatHelper {

    private DecimalFormat decimalFormat = new DecimalFormat("#.#####");

    public String formatWritable(Writable writable) {
      if (writable instanceof NullWritable) {
        return "NullWritable.get()";
      } else if (writable instanceof BooleanWritable) {
        return String.format("new BooleanWritable(%s)", format(((BooleanWritable) writable).get()));
      } else if (writable instanceof ByteWritable) {
        return String.format("new ByteWritable(%s)", format(((ByteWritable) writable).get()));
      } else if (writable instanceof IntWritable) {
        return String.format("new IntWritable(%s)", format(((IntWritable) writable).get()));
      } else if (writable instanceof LongWritable) {
        return String.format("new LongWritable(%s)", format(((LongWritable) writable).get()));
      } else if (writable instanceof FloatWritable) {
        return String.format("new FloatWritable(%s)", format(((FloatWritable) writable).get()));
      } else if (writable instanceof DoubleWritable) {
        return String.format("new DoubleWritable(%s)", format(((DoubleWritable) writable).get()));
      } else if (writable instanceof Text) {
        return String.format("new Text(%s)", ((Text) writable).toString());
      } else {
        complexWritables.add(writable.getClass());
        String str = toByteArrayString(WritableUtils.writeToByteArray(writable));
        return String.format("(%s)read%sFromByteArray(new byte[] {%s})", writable.getClass().getSimpleName(),
            writable.getClass().getSimpleName(), str);
      }
    }

    public String format(Object input) {
      if (input instanceof Boolean || input instanceof Byte || input instanceof Character
          || input instanceof Short || input instanceof Integer) {
        return input.toString();
      } else if (input instanceof Long) {
        return input.toString() + "l";
      } else if (input instanceof Float) {
        return decimalFormat.format(input) + "f";
      } else if (input instanceof Double) {
        double val = ((Double) input).doubleValue();
        if (val == Double.MAX_VALUE)
          return "Double.MAX_VALUE";
        else if (val == Double.MIN_VALUE)
          return "Double.MIN_VALUE";
        else {
          BigDecimal bd = new BigDecimal(val);
          return bd.toEngineeringString() + "d";
        }
      } else {
        return input.toString();
      }
    }
    
    private String toByteArrayString(byte[] byteArray) {
      StringBuilder strBuilder = new StringBuilder();
      for (int i = 0; i < byteArray.length; i++) {
        if (i != 0)
          strBuilder.append(',');
        strBuilder.append(Byte.toString(byteArray[i]));
      }
      return strBuilder.toString();
    }
  }
}