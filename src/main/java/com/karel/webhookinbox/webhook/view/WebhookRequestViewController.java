package com.karel.webhookinbox.webhook.view;

import com.karel.webhookinbox.common.web.IngestUrlResolver;
import com.karel.webhookinbox.inbox.application.InboxService;
import com.karel.webhookinbox.inbox.dto.InboxResponse;
import com.karel.webhookinbox.webhook.application.WebhookRequestService;
import com.karel.webhookinbox.webhook.dto.WebhookRequestDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class WebhookRequestViewController {

    private static final String LIST_FRAGMENT = "fragments/request-list :: list";

    private final InboxService inboxService;
    private final WebhookRequestService webhookRequestService;
    private final IngestUrlResolver ingestUrlResolver;

    @GetMapping("/inboxes/{token}/requests")
    public String list(@PathVariable String token,
                       @RequestParam(required = false) String method,
                       @RequestParam(required = false) String q,
                       Model model) {
        InboxResponse inbox = inboxService.getByToken(token);
        model.addAttribute("inbox", inbox);
        model.addAttribute("requests", webhookRequestService.list(inbox.id(), method, q, 0, 0).getContent());
        return LIST_FRAGMENT;
    }

    @GetMapping("/inboxes/{token}/requests/{id}")
    public String detail(@PathVariable String token, @PathVariable Long id, Model model) {
        InboxResponse inbox = inboxService.getByToken(token);
        WebhookRequestDetail request = webhookRequestService.get(inbox.id(), id);
        String ingestUrl = ingestUrlResolver.resolve(token);
        model.addAttribute("inbox", inbox);
        model.addAttribute("request", request);
        model.addAttribute("requestUrl", ingestUrl + request.path() + querySuffix(request.queryString()));
        model.addAttribute("curl", curlCommand(request, ingestUrl));
        return "request";
    }

    @PostMapping("/inboxes/{token}/requests/{id}/delete")
    public String delete(@PathVariable String token, @PathVariable Long id, Model model) {
        InboxResponse inbox = inboxService.getByToken(token);
        webhookRequestService.delete(inbox.id(), id);
        return renderList(inbox, model);
    }

    @PostMapping("/inboxes/{token}/requests/clear")
    public String clear(@PathVariable String token, Model model) {
        InboxResponse inbox = inboxService.getByToken(token);
        webhookRequestService.clear(inbox.id());
        return renderList(inbox, model);
    }

    private String renderList(InboxResponse inbox, Model model) {
        model.addAttribute("inbox", inbox);
        model.addAttribute("requests", webhookRequestService.list(inbox.id(), null, null, 0, 0).getContent());
        return LIST_FRAGMENT;
    }

    private String querySuffix(String queryString) {
        return (queryString == null || queryString.isBlank()) ? "" : "?" + queryString;
    }

    private String curlCommand(WebhookRequestDetail request, String ingestUrl) {
        StringBuilder curl = new StringBuilder("curl -X ")
                .append(request.method()).append(" '")
                .append(ingestUrl).append(request.path()).append(querySuffix(request.queryString())).append("'");
        request.headers().forEach((name, values) -> values.forEach(value ->
                curl.append(" \\\n  -H '").append(shellQuote(name)).append(": ").append(shellQuote(value)).append("'")));
        if (request.body() != null && !request.body().isEmpty()) {
            curl.append(" \\\n  --data-raw '").append(shellQuote(request.body())).append("'");
        }
        return curl.toString();
    }

    private String shellQuote(String value) {
        return value.replace("'", "'\\''");
    }
}
