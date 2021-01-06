package com.tabnine;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.tabnine.binary.requests.autocomplete.AutocompleteResponse;
import com.tabnine.binary.requests.autocomplete.ResultEntry;
import com.tabnine.general.DependencyContainer;
import com.tabnine.general.StaticConfig;
import com.tabnine.prediction.CompletionFacade;
import com.tabnine.prediction.TabNineCompletion;
import com.tabnine.prediction.TabNinePrefixMatcher;
import com.tabnine.selections.TabNineLookupListener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.tabnine.general.StaticConfig.*;
import static com.tabnine.general.Utils.endsWithADot;

public class FilteringCompletionContributor extends CompletionContributor {

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull final CompletionResultSet resultSet) {
        ArrayList<CompletionResult> elements = new ArrayList<>();

        // collect the other completion-contributors results
        resultSet.runRemainingContributors(parameters, elements::add);

        List<Integer> filteredsordedResults = IntStream.range(0, elements.size())
                .filter(i -> {
                    LookupElement tabnine_element = elements.get(i).getLookupElement();
                    if (tabnine_element.getObject() instanceof TabNineCompletion) {
                        String tabnine_autocompl = tabnine_element.getLookupString();

                        boolean already_in_other_contributers_results = elements.stream()
                                .filter(e -> !(e.getLookupElement().getObject() instanceof TabNineCompletion))
                                .anyMatch(e -> {
                                    String another_autocompl = e.getLookupElement().getLookupString();
                                    return another_autocompl.equals(tabnine_autocompl);
                                });
                        return !already_in_other_contributers_results;
                    } else {
                        return true;
                    }
                })
                .mapToObj(i -> (Integer) i) //WTF? I don't know how java works!
                .sorted(Comparator.comparing(i -> {
                    boolean is_tabnine = elements.get(i).getLookupElement().getObject() instanceof TabNineCompletion;
                    return is_tabnine? i + 10000 :i; // if is tabnine should go to the very back! behind all others!
                }))
                .collect(Collectors.toList());

        // add to resultSet
        filteredsordedResults.forEach(i -> resultSet.passResult(elements.get(i)));;

    }
}
