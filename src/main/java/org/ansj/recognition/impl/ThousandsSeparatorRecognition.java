package org.ansj.recognition.impl;

import org.ansj.domain.Result;
import org.ansj.domain.Term;
import org.ansj.domain.TermNature;
import org.ansj.domain.TermNatures;
import org.ansj.recognition.Recognition;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/** Recognizes decimal numbers written with a thousands separator. */
public class ThousandsSeparatorRecognition implements Recognition {

    private static final long serialVersionUID = 1L;
    private static final TermNatures THOUSANDS_NATURES = new TermNatures(new TermNature("thousands_separator", 1));

    private String separator = ",";
    private Pattern groupedNumber;

    public ThousandsSeparatorRecognition() {
        updatePattern();
    }

    public ThousandsSeparatorRecognition(String separator) {
        setSeparator(separator);
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        if (separator == null || separator.length() != 1 || Character.isDigit(separator.charAt(0))
                || ".".equals(separator)) {
            throw new IllegalArgumentException("The thousands separator must be one non-digit character other than a period.");
        }
        this.separator = separator;
        updatePattern();
    }

    @Override
    public void recognition(Result result) {
        List<Term> terms = result.getTerms();
        for (int i = 0; i < terms.size(); i++) {
            Term first = terms.get(i);
            if (first.getOffe() < 0 || !startsWithDigit(first.getName()) || !isNumberPart(first.getName())) {
                continue;
            }

            StringBuilder value = new StringBuilder();
            String firstName = first.getName();
            List<Term> parts = new ArrayList<Term>();
            List<Integer> prefixLengths = new ArrayList<Integer>();
            int cursor = i;
            Term previous = null;
            while (cursor < terms.size()) {
                Term part = terms.get(cursor);
                int prefixLength = numericPrefixLength(part.getName());
                if (part.getOffe() < 0 || prefixLength == 0
                        || (previous != null && !areAdjacent(previous, part))) {
                    break;
                }
                parts.add(part);
                prefixLengths.add(prefixLength);
                value.append(part.getName(), 0, prefixLength);
                previous = part;
                cursor++;
                if (prefixLength < part.getName().length()) {
                    break;
                }
            }

            if (groupedNumber.matcher(value).matches()) {
                for (int part = 1; part < parts.size(); part++) {
                    Term numberPart = parts.get(part);
                    int prefixLength = prefixLengths.get(part);
                    if (prefixLength == numberPart.getName().length()) {
                        first.merage(numberPart);
                        numberPart.setOffe(-1);
                    } else {
                        String suffix = numberPart.getName().substring(prefixLength);
                        Term numericTerm = new Term(numberPart.getName().substring(0, prefixLength),
                                numberPart.getOffe(), numberPart.termNatures());
                        first.merage(numericTerm);
                        int suffixOffset = numberPart.getOffe() + prefixLength;
                        numberPart.setName(suffix);
                        numberPart.setRealName(suffix);
                        numberPart.setOffe(suffixOffset);
                    }
                }

                int firstPrefixLength = prefixLengths.get(0);
                if (firstPrefixLength < firstName.length()) {
                    String suffix = firstName.substring(firstPrefixLength);
                    int suffixOffset = first.getOffe() + firstPrefixLength;
                    Term suffixTerm = new Term(suffix, suffixOffset, first.termNatures());
                    suffixTerm.setRealName(suffix);
                    first.setName(firstName.substring(0, firstPrefixLength));
                    first.setRealName(first.getName());
                    terms.add(cursor, suffixTerm);
                    cursor++;
                }
                first.updateTermNaturesAndNature(THOUSANDS_NATURES);
            }
            i = cursor - 1;
        }

        for (Iterator<Term> iterator = terms.iterator(); iterator.hasNext();) {
            if (iterator.next().getOffe() < 0) {
                iterator.remove();
            }
        }
    }

    private void updatePattern() {
        String groupedInteger = "[0-9]{1,3}(?:" + Pattern.quote(separator) + "[0-9]{3})+";
        groupedNumber = Pattern.compile(groupedInteger + "(?:\\.[0-9]+)?");
    }

    private int numericPrefixLength(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        int index = 0;
        while (index < value.length()) {
            char character = value.charAt(index);
            if ((character < '0' || character > '9') && character != separator.charAt(0) && character != '.') {
                break;
            }
            index++;
        }
        return index;
    }

    private static boolean startsWithDigit(String value) {
        return value != null && !value.isEmpty() && value.charAt(0) >= '0' && value.charAt(0) <= '9';
    }

    private static boolean areAdjacent(Term left, Term right) {
        return left.getOffe() + left.getName().length() == right.getOffe();
    }
}
