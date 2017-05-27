package com.polytech.web;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.polytech.business.*;
import com.polytech.models.*;
import com.polytech.repository.RequetMongoRepository;
import org.apache.tomcat.util.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.*;

/**
 * Created by dev on 3/15/17.
 */
@Controller
public class ApplicationController {

    @Autowired
    private SignInService signInService;

    @Autowired
    private CommunauteService communauteService;

    @Autowired
    private AdhesionService adhesionService;

    @Autowired
    private RechercheService rechercheService;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthorityService authorityService;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String index(Model model, Principal principal) throws UnsupportedEncodingException {
        String role = getRole(principal.getName());
        System.out.println("Role INDEX : " +  role);
        model.addAttribute("role", role);
        if(role.equals("RESPONSABLE")){
            Communaute communaute = communauteService.getCommunauteByResponsable(principal.getName());
            List<Adhesion> adhesions = adhesionService.findAdhesionByCommunaute(communaute.getId());
            model.addAttribute("nbDemandes", adhesions.size());
        }
        else {
            if(role.equals("MEMBRE")){
                User user = userService.findUserByUsername(principal.getName());
                Communaute communaute = communauteService.getCommunauteById(user.getIdCommunaute());
                model.addAttribute("communaute", communaute.getNom());
            }
        }
        return "index";
    }

    @RequestMapping(value = "/signup", method = RequestMethod.GET)
    public String signup(){
        return "signup";
    }

    @RequestMapping(value = "/dosignup", method = RequestMethod.POST)
    public String dosignup(String username, String password, Principal principal){
        signInService.signIn(new User(username, new BCryptPasswordEncoder().encode(password), 1));
        return "login";
    }

    @RequestMapping(value = "/creer", method = RequestMethod.GET)
    public String creer(Principal principal, Model model){
        String role = getRole(principal.getName());
        model.addAttribute("role", role);
        return "creer";
    }

    @RequestMapping(value = "/creerCommunaute", method = RequestMethod.POST)
    public String creerCommunaute(Communaute communaute, Principal principal){
        Communaute communaute1 = communauteService.getCommunauteByResponsable(principal.getName());
        if(communaute1==null) {
            communaute.setResponsableID(principal.getName());
            System.out.println(communaute.getNom());
            System.out.println(communaute.getResponsable());
            System.out.println(communaute.getDescription());
            communauteService.save(communaute);
            Authority authority = new Authority(principal.getName(), "RESPONSABLE");
            System.out.println(authority.getAuthority());
            authorityService.save(authority);
            for (Authority a : authorityService.selectAll()) {
                System.out.println(a.getAuthority());
            }
        }
        return "redirect:/";
    }

    @RequestMapping(value = "/gerer", method = RequestMethod.GET)
    public String gerer(Model model, Principal principal){
        String role = getRole(principal.getName());
        model.addAttribute("role", role);
        if(role.equals("RESPONSABLE")){
            Communaute communaute = communauteService.getCommunauteByResponsable(principal.getName());
            List<Adhesion> adhesions = adhesionService.findAdhesionByCommunaute(communaute.getId());
            model.addAttribute("nbDemandes", adhesions.size());
        }
        Communaute communaute=communauteService.getCommunauteByResponsable(principal.getName());
        List<User> users=userService.findUserByidCommunaute(communaute.getId());
        model.addAttribute("resultatsusers", users);
        model.addAttribute("resultats", communaute);

        return "gerer";
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public String search(Requete requete, Principal principal, Model model){
        requete.setUsername(principal.getName());
        rechercheService.saveRequete(requete);
        Crawler obj = new Crawler();
        List<Result> results = obj.getDataFromGoogle(requete.getQuery().replace(" ", "+"));
        for(Result temp : results){
            temp.setRequete(requete.getId());
        }
        model.addAttribute("resultats", results);
        return "index";
    }

    @RequestMapping(value = "/rate", method = RequestMethod.POST)
    public String rate(Requete requete, Principal principal, Model model){
        return "index";
    }

    @RequestMapping(value = "/click", method = RequestMethod.POST)
    public ModelAndView click(Result result, Principal principal, Model model){
        rechercheService.saveResultat(result);
        return new ModelAndView("redirect:" + result.getUri());
    }

    @RequestMapping(value = "/rejoindre/{id}")
    public String adherer(@PathVariable("id") String id, Principal principal){

            adhesionService.save(new Adhesion(id, principal.getName()));
        return "redirect:/rejoindre";
    }

    @RequestMapping(value = "/rejoindre")
    public String rejoindre(Principal principal, Model model){
        String role = getRole(principal.getName());
        model.addAttribute("role", role);
        List<Communaute> communautes = new ArrayList<>();

        User user = userService.findUserByUsername(principal.getName());
        if(user.getIdCommunaute()==null) {
            Adhesion adhesion = adhesionService.findAdhesionByUser(principal.getName());
            if (adhesion != null) {
                communautes.add(communauteService.getCommunauteById(adhesion.getIdCommunaute()));
                model.addAttribute("resultats2", communautes);
            } else {
                communautes = communauteService.selectAll();
                model.addAttribute("resultats", communautes);
            }
        }
        return "rejoindre";
    }

    @RequestMapping(value = "/deleteAdhesion/{id}")
    public String retirerAdhesion(@PathVariable("id") String id, Principal principal){
        String idAdhesion = adhesionService.findAdhesionByUser(principal.getName()).getId();
        adhesionService.delete(idAdhesion);
        return "redirect:/rejoindre";
    }

    @RequestMapping(value = "/gererDemandes")
    public String gererDemandes(Principal principal, Model model){
        String role = getRole(principal.getName());
        model.addAttribute("role", role);
        String idCommunaute = communauteService.getCommunauteByResponsable(principal.getName()).getId();
        List<Adhesion> adhesions = adhesionService.findAdhesionByCommunaute(idCommunaute);
        model.addAttribute("demandes", adhesions);
        return "gererDemandes";
    }

    @RequestMapping(value = "/approuver/{id}")
    public String approuverDamande(@PathVariable("id") String id, Principal principal){
        Communaute communaute = communauteService.getCommunauteByResponsable(principal.getName());
        Adhesion adhesion = adhesionService.findAdhesionByID(id);
        User user = userService.findUserByUsername(adhesion.getIdUtilisateur());
        user.setIdCommunaute(communaute.getId());
        userService.save(user);
        adhesionService.delete(adhesion.getId());
        Authority authority = new Authority(user.getUsername(),"MEMBRE");
        authorityService.save(authority);
        return "redirect:/gererDemandes";
    }


    @RequestMapping(value = "/desapprouver/{id}")
    public String desapprouverDamande(@PathVariable("id") String id, Principal principal){
        Adhesion adhesion = adhesionService.findAdhesionByID(id);
        adhesionService.delete(adhesion.getId());
        return "redirect:/gererDemandes";
    }


    @RequestMapping(value = "/deleteOrUpdateCommunaute")
    public String deleteOrUpdateCommunaute(Principal principal, Model model){
        String role = getRole(principal.getName());
        model.addAttribute("role", role);
        Communaute communaute=communauteService.getCommunauteByResponsable(principal.getName());
        model.addAttribute("communaute",communaute);
        return "deleteOrUpdateCommunaute";
    }



    //
    @RequestMapping(value = "/deleteCommunaute/{id}")
    public String deleteCommunaute(@PathVariable("id") String id, Principal principal){

      List<User> users= userService.findUserByidCommunaute(id);
        for (User user: users) {
            user.setIdCommunaute(null);
            userService.save(user);
            Authority authority=new Authority();
            authority.setAuthority("USER");
            authority.setUsername(user.getUsername());
            authorityService.save(authority);
        }

        communauteService.delete(id);
        Authority authority=new Authority();
        authority.setAuthority("USER");
        authority.setUsername(principal.getName());
        authorityService.save(authority);


        return "redirect:/";
    }

    @RequestMapping(value = "/UpdateCommunaute", method = RequestMethod.POST)
    public String UpdateCommunaute(Communaute communaute, Principal principal){
        communaute.setResponsableID(principal.getName());
        System.out.println(communaute.getNom());
        System.out.println(communaute.getId());
        System.out.println(communaute.getDescription());
        communauteService.save(communaute);
        return "redirect:/gerer";
    }
    //
    @RequestMapping(value = "/Update", method = RequestMethod.POST)
    public String Update(){
        return  "redirect:/deleteOrUpdateCommunaute";
    }

    public String getRole(String username){
        Authority authority = authorityService.getAuthorityByUsername(username);
        return authority.getAuthority();
    }





    @RequestMapping(value = "/deleteUserFromCommunaute/{id}")
    public String deleteUserFromCommunaute(@PathVariable("id") String id, Principal principal){
        //
        User user = userService.findUserByUsername(id);
        user.setIdCommunaute(null);
        userService.save(user);
        Authority authority=new Authority();
        authority.setAuthority("USER");
        authority.setUsername(user.getUsername());
        authorityService.save(authority);
        return "redirect:/gerer";
    }

    @RequestMapping(value = "/QuiteCommunaute")
    public String QuiteCommunaute(Principal principal){
        //
        User user = userService.findUserByUsername(principal.getName());
        user.setIdCommunaute(null);
        userService.save(user);
        Authority authority=new Authority();
        authority.setAuthority("USER");
        authority.setUsername(user.getUsername());
        authorityService.save(authority);
        return "redirect:/";
    }

}
