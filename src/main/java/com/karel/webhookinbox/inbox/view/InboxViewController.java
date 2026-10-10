package com.karel.webhookinbox.inbox.view;

import com.karel.webhookinbox.common.error.ValidationException;
import com.karel.webhookinbox.common.web.IngestUrlResolver;
import com.karel.webhookinbox.inbox.application.InboxService;
import com.karel.webhookinbox.inbox.dto.InboxResponse;
import com.karel.webhookinbox.webhook.application.WebhookRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class InboxViewController {

    private final InboxService inboxService;
    private final WebhookRequestService webhookRequestService;
    private final IngestUrlResolver ingestUrlResolver;

    @GetMapping("/")
    public String home() {
        return "redirect:/inboxes";
    }

    @GetMapping("/inboxes")
    public String list(Model model) {
        model.addAttribute("inboxes", inboxService.list());
        return "inboxes";
    }

    @PostMapping("/inboxes")
    public String create(@RequestParam String name,
                         @RequestParam(required = false) String description,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        try {
            InboxResponse created = inboxService.create(name, description);
            redirectAttributes.addFlashAttribute("message", "Inbox created");
            return "redirect:/inboxes/" + created.token();
        } catch (ValidationException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("inboxes", inboxService.list());
            return "inboxes";
        }
    }

    @GetMapping("/inboxes/{token}")
    public String detail(@PathVariable String token, Model model) {
        InboxResponse inbox = inboxService.getByToken(token);
        model.addAttribute("inbox", inbox);
        model.addAttribute("ingestUrl", ingestUrlResolver.resolve(token));
        model.addAttribute("requests", webhookRequestService.list(inbox.id(), null, null, 0, 0).getContent());
        return "inbox";
    }

    @PostMapping("/inboxes/{token}/delete")
    public String delete(@PathVariable String token, RedirectAttributes redirectAttributes) {
        inboxService.delete(token);
        redirectAttributes.addFlashAttribute("message", "Inbox deleted");
        return "redirect:/inboxes";
    }
}
