package github.lms.lemuel.game.adapter.in.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/games")
public class GameController {

    @GetMapping("/baduk")
    public String baduk() {
        return "baduk";
    }

    @GetMapping("/omok")
    public String omok() {
        return "omok";
    }
}