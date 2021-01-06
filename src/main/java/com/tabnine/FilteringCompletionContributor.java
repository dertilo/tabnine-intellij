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
import java.util.Objects;
import java.util.stream.IntStream;

import static com.tabnine.general.StaticConfig.*;
import static com.tabnine.general.Utils.endsWithADot;

public class FilteringCompletionContributor extends CompletionContributor {

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull final CompletionResultSet resultSet) {
        ArrayList<CompletionResult> elements = new ArrayList<>();

        resultSet.runRemainingContributors(parameters,r -> {
            System.out.println(r.toString());
            elements.add(r);
        });

        //elements.stream().filter(r -> !(r.getLookupElement().getObject() instanceof TabNineCompletion)).forEach(e -> resultSet.passResult(e));
        IntStream.range(0,elements.size())
                .filter(i -> {
                    if(elements.get(i).getLookupElement().getObject() instanceof TabNineCompletion){
                        TabNineCompletion tabnine = (TabNineCompletion) elements.get(i).getLookupElement().getObject();
                        boolean is_redundant = elements.stream()
                                .filter(e -> !(e.getLookupElement().getObject() instanceof TabNineCompletion))
                                .anyMatch(e -> {
                                    String a = e.getLookupElement().getLookupString();
                                    String b = elements.get(i).getLookupElement().getLookupString();
                                    return a.equals(b);
                                });
                        return ! is_redundant;
                    } else {
                        return true;
                    }
                })
                .mapToObj(i-> (Integer) i)
                .sorted(Comparator.comparing(i -> {
                    return elements.get(i).getLookupElement().getObject() instanceof TabNineCompletion ? i + Integer.MAX_VALUE : i;
                }))
                .forEach(i -> resultSet.passResult(elements.get(i)));

    }
}
