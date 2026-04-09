package mchorse.bbs_mod.ui.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UIAIMessageParser
{
    private static final String QUESTION_TAG = "question-card";
    private static final String TODO_TAG = "todo-card";
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("(\\w+)\\s*=\\s*\"([^\"]*)\"");

    public static List<Segment> parse(String text)
    {
        List<Segment> segments = new ArrayList<>();

        if (text == null || text.isEmpty())
        {
            return segments;
        }

        int index = 0;

        while (index < text.length())
        {
            int questionIndex = text.indexOf("<" + QUESTION_TAG, index);
            int todoIndex = text.indexOf("<" + TODO_TAG, index);
            int nextIndex = nextIndex(questionIndex, todoIndex);

            if (nextIndex < 0)
            {
                appendText(segments, text.substring(index));
                break;
            }

            appendText(segments, text.substring(index, nextIndex));

            if (nextIndex == questionIndex)
            {
                BlockResult<QuestionCardData> result = parseQuestion(text, questionIndex);

                if (result == null)
                {
                    appendText(segments, text.substring(questionIndex));
                    break;
                }

                segments.add(Segment.question(result.data));
                index = result.nextIndex;
            }
            else
            {
                BlockResult<TodoCardData> result = parseTodo(text, todoIndex);

                if (result == null)
                {
                    appendText(segments, text.substring(todoIndex));
                    break;
                }

                segments.add(Segment.todo(result.data));
                index = result.nextIndex;
            }
        }

        return segments;
    }

    private static int nextIndex(int a, int b)
    {
        if (a < 0) return b;
        if (b < 0) return a;

        return Math.min(a, b);
    }

    private static void appendText(List<Segment> segments, String text)
    {
        String trimmed = text.trim();

        if (!trimmed.isEmpty())
        {
            segments.add(Segment.text(trimmed));
        }
    }

    private static BlockResult<QuestionCardData> parseQuestion(String text, int start)
    {
        String openTag = "<" + QUESTION_TAG;
        String closeTag = "</" + QUESTION_TAG + ">";
        int headerEnd = text.indexOf(">", start);
        int closeIndex = text.indexOf(closeTag, start);

        if (headerEnd < 0 || closeIndex < 0 || closeIndex <= headerEnd)
        {
            return null;
        }

        String attributes = text.substring(start + openTag.length(), headerEnd);
        String body = text.substring(headerEnd + 1, closeIndex).trim();
        QuestionCardData data = new QuestionCardData();

        data.title = getAttribute(attributes, "title", "Choose an option");
        data.prompt = firstNonEmptyLine(body);
        data.multi = Boolean.parseBoolean(getAttribute(attributes, "multi", "false"));
        data.submitLabel = getAttribute(attributes, "submit", data.multi ? "Send choices" : "Use this option");

        for (String line : body.split("\\r?\\n"))
        {
            String trimmed = line.trim();

            if (trimmed.startsWith("- "))
            {
                data.options.add(trimmed.substring(2).trim());
            }
        }

        return data.options.isEmpty() ? null : new BlockResult<>(data, closeIndex + closeTag.length());
    }

    private static BlockResult<TodoCardData> parseTodo(String text, int start)
    {
        String openTag = "<" + TODO_TAG;
        String closeTag = "</" + TODO_TAG + ">";
        int headerEnd = text.indexOf(">", start);
        int closeIndex = text.indexOf(closeTag, start);

        if (headerEnd < 0 || closeIndex < 0 || closeIndex <= headerEnd)
        {
            return null;
        }

        String attributes = text.substring(start + openTag.length(), headerEnd);
        String body = text.substring(headerEnd + 1, closeIndex).trim();
        TodoCardData data = new TodoCardData();

        data.title = getAttribute(attributes, "title", "Plan");

        for (String line : body.split("\\r?\\n"))
        {
            String trimmed = line.trim();

            if (trimmed.startsWith("[ ] "))
            {
                data.items.add(new TodoItem(TodoState.PENDING, trimmed.substring(4).trim()));
            }
            else if (trimmed.startsWith("[>] "))
            {
                data.items.add(new TodoItem(TodoState.ACTIVE, trimmed.substring(4).trim()));
            }
            else if (trimmed.startsWith("[x] ") || trimmed.startsWith("[X] "))
            {
                data.items.add(new TodoItem(TodoState.DONE, trimmed.substring(4).trim()));
            }
        }

        return data.items.isEmpty() ? null : new BlockResult<>(data, closeIndex + closeTag.length());
    }

    private static String firstNonEmptyLine(String body)
    {
        for (String line : body.split("\\r?\\n"))
        {
            String trimmed = line.trim();

            if (!trimmed.isEmpty() && !trimmed.startsWith("- "))
            {
                return trimmed;
            }
        }

        return "";
    }

    private static String getAttribute(String attributes, String key, String fallback)
    {
        Matcher matcher = ATTRIBUTE_PATTERN.matcher(attributes);

        while (matcher.find())
        {
            if (key.equals(matcher.group(1)))
            {
                return matcher.group(2);
            }
        }

        return fallback;
    }

    private static class BlockResult<T>
    {
        public final T data;
        public final int nextIndex;

        public BlockResult(T data, int nextIndex)
        {
            this.data = data;
            this.nextIndex = nextIndex;
        }
    }

    public static class Segment
    {
        public final SegmentType type;
        public final String text;
        public final QuestionCardData question;
        public final TodoCardData todo;

        private Segment(SegmentType type, String text, QuestionCardData question, TodoCardData todo)
        {
            this.type = type;
            this.text = text;
            this.question = question;
            this.todo = todo;
        }

        public static Segment text(String text)
        {
            return new Segment(SegmentType.TEXT, text, null, null);
        }

        public static Segment question(QuestionCardData question)
        {
            return new Segment(SegmentType.QUESTION, null, question, null);
        }

        public static Segment todo(TodoCardData todo)
        {
            return new Segment(SegmentType.TODO, null, null, todo);
        }
    }

    public enum SegmentType
    {
        TEXT, QUESTION, TODO
    }

    public static class QuestionCardData
    {
        public String title;
        public String prompt;
        public boolean multi;
        public String submitLabel;
        public final List<String> options = new ArrayList<>();
    }

    public static class TodoCardData
    {
        public String title;
        public final List<TodoItem> items = new ArrayList<>();
    }

    public static class TodoItem
    {
        public final TodoState state;
        public final String text;

        public TodoItem(TodoState state, String text)
        {
            this.state = state;
            this.text = text;
        }
    }

    public enum TodoState
    {
        PENDING, ACTIVE, DONE
    }
}
