import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Probability {
    private static String expression;
    private static LinkedList<TreeMap<Integer, BigDecimal>> list = new LinkedList<>();

    public static void main(String[] args) throws IOException {
        Scanner in = new Scanner(System.in);
        //String line = in.nextLine();
        //String line = "d3*(d4+(d8>3))*((d15>10)-4)-(d2>1)+d3";
        //String line = "(d3*(d4+d8>3)*((d15>10)+(4+d3))-(d2>1))";
        //String line = "d3*(d4+d8>3)*((d15>10)+(4+d3))";
        //String line = "(d5>4)+(d3-d7)-(d5+d2)*(3+d6)";
        String line = "(d13>6)+10>d5";
        line = line.replaceAll(" ","");

        LinkedList<String> operands = Arrays
                .stream(line.split("[>*+\\-\\(\\)]+"))
                .filter(x -> !x.isEmpty())
                .collect(Collectors.toCollection(LinkedList<String>::new));

        operands.forEach(x -> list.add(parse(x)));

        expression = createExpression(line);

        if(expression.contains("("))
            openBraces();

        LinkedList<String> operators = Arrays.stream(expression.split("[^+*>-]")).filter(x -> !x.isEmpty()).collect(Collectors.toCollection(LinkedList<String>::new));

        if(!operators.isEmpty()){
            if(operators.contains("*")) {
                multiplyAction(operators,expression);
            }
            if(operators.contains("+") || operators.contains("-")) {
                plusMinusAction(operators,expression);
            }
            if(operators.contains(">")) {
                boolAction(operators,expression);
            }

        }

        for (Map.Entry entry : list.get(list.size()-1).entrySet()) {
            BigDecimal x = (BigDecimal) entry.getValue();
            x = x.multiply(BigDecimal.valueOf(100l)).setScale(2, RoundingMode.HALF_UP);
            System.out.println(entry.getKey() + " " + x);
        }


    }
    public static String createExpression (String ex) {
        String expression = ex.replaceAll("[^>+*\\-\\(\\)]","X").replaceAll("X+","X");

        StringBuilder sb = new StringBuilder();
        int counter = 0;
        for (int i = 0; i < expression.length(); i++) {
            if(expression.charAt(i) == 'X'){
                sb.append(counter);
                counter++;
            }
            else
                sb.append(expression.charAt(i));
        }

        return sb.toString();
    }

    public static TreeMap<Integer, BigDecimal> parse (String operand) {

        int n = operand.contains("d") ? Integer.parseInt(operand.substring(1)) : Integer.parseInt(operand);
        TreeMap <Integer, BigDecimal> result = new TreeMap<>();

        if(operand.contains("d")) {
            for (int i = 1; i <= n; i++) {
                result.merge(i, BigDecimal.valueOf(1.0d/n).setScale(10, RoundingMode.HALF_UP), (oldValue, newValue) -> oldValue.add(newValue));
            }
        } else
            result.put(n,BigDecimal.valueOf(1.0));

        return result;
    }

    public static void openBraces () {

        Pattern pattern = Pattern.compile("\\([^\\(\\)]+\\)");
        Matcher matcher = pattern.matcher(expression);

        while (matcher.find()) {
            String find = matcher.group();

            if(find.replaceAll("\\(","").replaceAll("\\)","").matches("\\d+")) {
                expression = expression.replace(find,find.replaceAll("\\(","").replaceAll("\\)",""));
                matcher.reset(expression);
            }

            List<String> tempList = Arrays
                    .stream(find.split("[^>*+-]"))
                    .filter(x -> !x.isEmpty())
                    .collect(Collectors.toList());

            if(tempList.contains("*")) {
                multiplyAction(tempList, find);
            }

            if(tempList.contains("+") || tempList.contains("-"))
                plusMinusAction(tempList, find);

            if(tempList.contains(">"))
                boolAction(tempList, find);

            matcher.reset(expression);
        }
    }

    public static int multiplyAction (List<String> operators, String find) {
        for (int i = 0; i < operators.size(); i++) {
            if(operators.get(i).equals("*")) {
                String toReplace = findReplaceEx(find,"*");
                if(toReplace.equals("10*11")){
                }

                List<String> ops = Arrays.stream(toReplace.split("[^\\d]+")).filter(x -> !x.isEmpty()).collect(Collectors.toList());
                TreeMap<Integer, BigDecimal> a = list.get(Integer.parseInt(ops.get(0)));
                TreeMap<Integer, BigDecimal> b = list.get(Integer.parseInt(ops.get(1)));

                operators.remove(i);
                list.add(action(a, b, "*"));
                expression = expression.replace(toReplace,String.valueOf(list.size()-1));
                find = expression;
                i--;
            }
        }
        return list.size()-1;
    }

    public static int plusMinusAction (List<String> operators, String find) {

        for (int i = 0; i < operators.size(); i++) {

            if(operators.get(i).equals("+") || operators.get(i).equals("-")) {
                String actionType = operators.get(i).equals("+") ?  "+" : "-";
                String toReplace = findReplaceEx(find,actionType);
                List<String> ops = Arrays.stream(toReplace.split("[^\\d]+")).filter(x -> !x.isEmpty()).collect(Collectors.toList());
                TreeMap<Integer, BigDecimal> a = list.get(Integer.parseInt(ops.get(0)));
                TreeMap<Integer, BigDecimal> b = list.get(Integer.parseInt(ops.get(1)));

                operators.remove(i);
                list.add(action(a, b, actionType));
                expression = expression.replace(toReplace,String.valueOf(list.size()-1));
                find = expression;
                i--;
            }
        }
        return list.size()-1;
    }
    public static int boolAction (List<String> operators, String find) {

        for (int i = 0; i < operators.size(); i++) {
            String toReplace = findReplaceEx(find,">");
            List<String> ops = Arrays.stream(toReplace.split("[^\\d]+")).filter(x -> !x.isEmpty()).collect(Collectors.toList());
            TreeMap<Integer, BigDecimal> a = list.get(Integer.parseInt(ops.get(0)));
            TreeMap<Integer, BigDecimal> b = list.get(Integer.parseInt(ops.get(1)));

            operators.remove(i);
            list.add(action(a, b, ">"));
            expression = expression.replace(toReplace,String.valueOf(list.size()-1));
            find = expression;
            i--;
        }
        return list.size()-1;
    }

    public static String findReplaceEx (String find, String actionType) {
        Pattern pattern = actionType.equals("*") ? Pattern.compile("[d\\d]+\\*[d\\d]+") :  Pattern.compile("[d\\d]+[+->][d\\d]+");
        Matcher matcher = pattern.matcher(find);
        if(matcher.find())
            return matcher.group();
        return "";
    }

    private static TreeMap<Integer, BigDecimal> parseBoolean(TreeMap<Integer, BigDecimal> a, TreeMap<Integer, BigDecimal> b) {

        TreeMap <Integer, BigDecimal> result = new TreeMap<>();

        int leftOperand = a.lastKey();
        int rightOperand = b.lastKey();

        BigDecimal probability = null;
        BigDecimal countZero = null;
        BigDecimal countOne = null;

        if(a.size() == 1 && b.size() > 1) {

            if(leftOperand > rightOperand) {
                result.put(1, BigDecimal.valueOf(1.0));
                return result;
            }
            else if(leftOperand <= 1) {
                result.put(0, BigDecimal.valueOf(1.0));
                return result;
            }
            else {
                probability = BigDecimal.valueOf(1.0/rightOperand).setScale(10, RoundingMode.HALF_UP);
                countOne = BigDecimal.valueOf(leftOperand - 1);
                countZero = BigDecimal.valueOf(rightOperand - countOne.intValue());
            }
        }

        if(b.size() == 1 && a.size() > 1) {
            if(rightOperand > leftOperand) {
                result.put(0, BigDecimal.valueOf(1.0));
                return result;
            }
            else if(rightOperand < 1) {
                result.put(1, BigDecimal.valueOf(1.0));
                return result;
            }
            else {
                probability = BigDecimal.valueOf(1.0/leftOperand).setScale(10, RoundingMode.HALF_UP);
                countOne = BigDecimal.valueOf(leftOperand - rightOperand);
                countZero = BigDecimal.valueOf(leftOperand - countOne.intValue());
            }
        }

        if(a.size() == 1 && b.size() == 1) {
            int zeroOrOne = leftOperand > rightOperand ? 1 : 0;
            result.put(zeroOrOne,BigDecimal.valueOf(1.0));
            return result;
        }

        if(a.size() > 1 && b.size() > 1) {
            int events = a.size() * b.size();
            int oneCounter = 0;
            int zeroCounter = 0;
            probability = BigDecimal.valueOf(1.0/events).setScale(10, RoundingMode.HALF_UP);

            for (Map.Entry leftEntry : a.entrySet()) {
                for (Map.Entry rightEntry : b.entrySet()){
                    BigDecimal x = (BigDecimal) leftEntry.getValue();
                    BigDecimal y = (BigDecimal) rightEntry.getValue();
                    x = x.setScale(10, RoundingMode.HALF_UP);
                    y = y.setScale(10, RoundingMode.HALF_UP);

                    if((int) leftEntry.getKey() > (int) rightEntry.getKey()) {
                        oneCounter++;
                    }
                    else
                        zeroCounter++;
                }
            }
            countZero = BigDecimal.valueOf(zeroCounter);
            countOne = BigDecimal.valueOf(oneCounter);
        }

        result.put(0, probability.multiply(countZero).setScale(10, RoundingMode.HALF_UP));
        result.put(1, probability.multiply(countOne).setScale(10, RoundingMode.HALF_UP));
        return result;
    }

    public static TreeMap<Integer, BigDecimal> action(TreeMap<Integer, BigDecimal> a, TreeMap<Integer, BigDecimal> b, String actionType) {
        TreeMap <Integer, BigDecimal> result = new TreeMap<>();

        if(actionType.equals(">"))
            return parseBoolean(a,b);

        for (Map.Entry entry : a.entrySet()) {
            for (Map.Entry entry2 : b.entrySet()){
                BigDecimal x = (BigDecimal) entry.getValue();
                BigDecimal y = (BigDecimal) entry2.getValue();
                x = x.setScale(10, RoundingMode.HALF_UP);
                y = y.setScale(10, RoundingMode.HALF_UP);

                switch (actionType) {
                    case "+":
                        result.merge((int) entry.getKey() + (int) entry2.getKey(),
                                x.multiply(y).setScale(10, RoundingMode.HALF_UP), (oldValue, newValue) -> oldValue.add(newValue).setScale(10, RoundingMode.HALF_UP));
                        break;
                    case "-":
                        result.merge((int) entry.getKey() - (int) entry2.getKey(),
                                x.multiply(y).setScale(10, RoundingMode.HALF_UP), (oldValue, newValue) -> oldValue.add(newValue).setScale(10, RoundingMode.HALF_UP));
                        break;
                    case "*":
                        result.merge((int) entry.getKey() * (int) entry2.getKey(),
                                x.multiply(y).setScale(10, RoundingMode.HALF_UP), (oldValue, newValue) -> oldValue.add(newValue).setScale(10, RoundingMode.HALF_UP));
                        break;
                }
            }
        }
        return result;
    }
}