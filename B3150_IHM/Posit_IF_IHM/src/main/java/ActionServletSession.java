/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import posit_if.DAO.JPAUtil;
import posit_if.OM.Client;
import posit_if.OM.Employe;
import posit_if.OM.Medium;
import posit_if.Service.Service;
/**
 *
 * @author woccelli
 */
@WebServlet(urlPatterns = {"/ActionServlet"})
public class ActionServletSession extends HttpServlet {

    
   
    @Override
    public void init()
            throws ServletException {
        super.init(); //To change body of generated methods, choose Tools | Templates.
        JPAUtil.init();
    }

    @Override
    public void destroy() {
        super.destroy(); //To change body of generated methods, choose Tools | Templates.
        JPAUtil.destroy();
    }
    
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(true);
        response.setContentType("application/json;charset=UTF-8");
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd");
        String action = request.getParameter("action");
        
        if("connecter".equals(action)){
            String user = request.getParameter("login");
            try(PrintWriter out = response.getWriter()){
                Client c = Service.authentificationClient(user);
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonObject container = new JsonObject();
                             
                if( c != null){
                    System.out.println(user);
                    session.setAttribute("user",user);
                    session.setAttribute("type","client");
                    JsonObject jsonClient = new JsonObject();
                    container.addProperty("status", true);
                    session.setAttribute("id",c.getId());
                    session.setAttribute("client",c);
                    container.add("client",jsonClient);
                    System.out.println(jsonClient);
                }
                else{
                    JsonObject jsonClient = new JsonObject();
                    jsonClient.addProperty("status", false);
                    container.add("client",jsonClient);
                }
                out.println(gson.toJson(container));
            }
        }
        else if ("inscrire".equals(action)) {
            String civilite = request.getParameter("civilite");
            String nom = request.getParameter("nom");
            String prenom = request.getParameter("prenom");
            String dateNaissance = request.getParameter("dateNaissance");
            String adresse = request.getParameter("adresse");
            String mail = request.getParameter("mail");
            String mailConfirmation = request.getParameter("mailConfirmation");
            String telephone = request.getParameter("telephone");
            String cgu = request.getParameter("cgu");
            
            try(PrintWriter out = response.getWriter()){
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonObject container = new JsonObject(); 
                JsonObject jsonClient = new JsonObject();
                if (cgu.equals("true") && mail.equals(mailConfirmation) && !(nom.equals("") ||prenom.equals("") ||adresse.equals("") ||civilite.equals("") ||mail.equals("") ||telephone.equals(""))){
                    Date d;
                    try {
                        d = sf.parse(dateNaissance);
                        Client c = new Client(nom,prenom,civilite,d,mail,telephone,adresse);
                        Service.inscriptionClient(c);
                        Service.envoyerMail(true, c);
                        jsonClient.addProperty("status", true);
                        container.add("client",jsonClient);
                    } catch (ParseException ex) {
                        Logger.getLogger(ActionServletSession.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else if(cgu.equals("false")){
                    jsonClient.addProperty("status", false);
                    jsonClient.addProperty("cause", "cgu");
                    container.add("client",jsonClient);
                } else if(!(mail.equals(mailConfirmation))){
                    jsonClient.addProperty("status", false);
                    jsonClient.addProperty("cause", "mail");
                    container.add("client",jsonClient);
                } else{
                    jsonClient.addProperty("status", false);
                    jsonClient.addProperty("cause", "champ vide");
                    container.add("client",jsonClient);
                }
                out.println(gson.toJson(container));
            }
        }
        else if ("connecter-employe".equals(action)){
            String user = request.getParameter("login");
            try(PrintWriter out = response.getWriter()){
                Employe e = Service.authentificationEmploye(Integer.parseInt(user));
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonObject container = new JsonObject();
                             
                if( e != null){
                    session.setAttribute("id",e);
                    session.setAttribute("type","employe");
                    JsonObject jsonEmploye = new JsonObject();
                    container.addProperty("status", true);
                    session.setAttribute("employe",e);
                    container.add("employe",jsonEmploye);
                }
                else{
                    JsonObject jsonEmploye = new JsonObject();
                    jsonEmploye.addProperty("status", false);
                    container.add("employe",jsonEmploye);
                }
                out.println(gson.toJson(container));
            }
        }
        else if ("afficherMedium".equals(action)){
            String nomMedium = request.getParameter("nomMedium");
            try(PrintWriter out = response.getWriter()){
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonObject container = new JsonObject();
                String res = Service.consulterMedium(nomMedium);
                container.addProperty("infosMedium",res);
                session.setAttribute("nom_medium_selectionne", nomMedium);
                out.println(gson.toJson(container));
            }
        }
        else if ("demanderVoyance".equals(action)) {
            Medium m = Service.trouverMedium((String)session.getAttribute("nom_medium_selectionne"));
            Client c = (Client)session.getAttribute("client");
            try(PrintWriter out = response.getWriter()){
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonObject jsonClient = new JsonObject();
                JsonObject container = new JsonObject(); 
                if(m != null && c !=null) {
                    if(Service.demandeVoyance(c, m)){
                        container.addProperty("status", true);
                        Service.recevoirNotifications(m, c);
                    }
                    else {
                        container.addProperty("status", false);
                    }    
                }
                else {
                    container.addProperty("status", false);
                }
                out.println(gson.toJson(container));
            }
        }
        else if ("initialiser-profilClient".equals(action)){
            Client client = (Client)session.getAttribute("client");
            try(PrintWriter out = response.getWriter()){
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonObject jsonClient = new JsonObject();
                JsonObject container = new JsonObject(); 
                if(client != null) {

                    container.addProperty("status", true);
                    jsonClient.addProperty("nom", client.getNom());
                    jsonClient.addProperty("prenom", client.getPrenom());
                    jsonClient.addProperty("adresse", client.getAddresse());
                    jsonClient.addProperty("mail", client.getMail());
                    jsonClient.addProperty("tel", client.getTel());
                    jsonClient.addProperty("signe_chinois", client.getSigne_ch());
                    jsonClient.addProperty("coul_fav", client.getCouleur());
                    jsonClient.addProperty("zodiaque", client.getSigne_z());
                    jsonClient.addProperty("animal_totem", client.getTotem());
                    jsonClient.addProperty("date_Naiss", sf.format(client.getDateNaissance()));

                    container.add("client",jsonClient);
                    out.println(gson.toJson(container));
                }
                else {
                   container.addProperty("status", false);
                   out.println(gson.toJson(container));
                }
            }
        }
        else if ("initialiser-historique".equals(action)) {
            try(PrintWriter out = response.getWriter()){
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonObject container = new JsonObject(); 
                Client client = (Client)session.getAttribute("client");
                if(client != null) {
                    container.addProperty("status", true);
                    JsonArray jsonListeConversations = new JsonArray();
                    
                    for(String[] s:Service.historiqueClient(client,false)){
                        JsonObject jsonConversation = new JsonObject();
                        jsonConversation.addProperty("medium",s[0]);
                        jsonConversation.addProperty("date",s[1]);
                        jsonConversation.addProperty("heureDeb",s[2]);
                        jsonConversation.addProperty("heureFin",s[3]);
                        jsonListeConversations.add(jsonConversation);
                    }
                    container.add("conversations",jsonListeConversations);
                }
                else {
                   container.addProperty("status", false); 
                }
                out.println(gson.toJson(container));
            }
        }
        else if ("modifierProfil".equals(action)) {
            try(PrintWriter out = response.getWriter()){
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonObject container = new JsonObject(); 
                Client client = (Client)session.getAttribute("client");
                if(request.getParameter("type").equals("coords")){
                    client.setAddresse(request.getParameter("adresse"));
                    client.setMail(request.getParameter("mail"));
                    client.setTel(request.getParameter("tel"));
                    if(Service.modifierCoordonneesClient(client) != null){
                        container.addProperty("status", true);
                    } else {
                        container.addProperty("status", false);
                    }
                        
                } else {
                    client.setCivilite(request.getParameter("civilite"));
                    client.setNom(request.getParameter("nom"));
                    client.setPrenom(request.getParameter("prenom"));
                    try {
                        client.setDateNaissance(sf.parse(request.getParameter("dateNaissance")));
                    } catch (ParseException ex) {
                        Logger.getLogger(ActionServletSession.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if( Service.modifierIdentiteClient(client) != null){
                        container.addProperty("status", true);
                    } else {
                        container.addProperty("status", false);
                    }
                   

                }
                out.println(gson.toJson(container));
            } 
        }
        
        
        else{
            if("initialiser-voyance".equals(action)){
                try(PrintWriter out = response.getWriter()){
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonObject container = new JsonObject(); 
                JsonArray jsonListeVoyants = new JsonArray();
                JsonArray jsonListeAstrologues = new JsonArray();
                JsonArray jsonListeCartomanciens = new JsonArray();
                for(String s:Service.listeVoyants()){
                    JsonObject jsonVoyant = new JsonObject();
                    jsonVoyant.addProperty("nom",s);
                    jsonListeVoyants.add(jsonVoyant);
                }
                for(String s:Service.listeAstrologues()){
                    JsonObject jsonAstrologue = new JsonObject();
                    jsonAstrologue.addProperty("nom",s);
                    jsonListeAstrologues.add(jsonAstrologue);
                }
                for(String s:Service.listeTarologues()){
                    JsonObject jsonCartomancien = new JsonObject();
                    jsonCartomancien.addProperty("nom",s);
                    jsonListeCartomanciens.add(jsonCartomancien);
                }
                container.add("voyants",jsonListeVoyants);
                container.add("astrologues",jsonListeAstrologues);
                container.add("cartomanciens",jsonListeCartomanciens);
                out.println(gson.toJson(container));
                }
            }
            
            else if("initialiser-consultation".equals(action)){
                Employe e = (Employe)session.getAttribute("employe");
                
                try(PrintWriter out = response.getWriter()){
                    Client c = Service.trouverClientEnAttente(e) ;
                    session.setAttribute("ClientEnAttente",c);
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    JsonObject container = new JsonObject();
                    if(e == null){
                        container.addProperty("connexion", false);
                    }
                    else{
                        container.addProperty("connexion", true);
                        if( c != null){
                            JsonObject jsonClient = new JsonObject();
                            container.addProperty("status",true);
                            JsonArray jsonListeHistorique = new JsonArray();
                            jsonClient.addProperty("status", true);                            
                            jsonClient.addProperty("civilite", c.getCivilite());
                            jsonClient.addProperty("nom", c.getNom());
                            jsonClient.addProperty("prenom", c.getPrenom());
                            jsonClient.addProperty("adresse", c.getAddresse());
                            jsonClient.addProperty("mail", c.getMail());
                            jsonClient.addProperty("tel", c.getTel());
                            jsonClient.addProperty("signe_chinois", c.getSigne_ch());
                            jsonClient.addProperty("coul_fav", c.getCouleur());
                            jsonClient.addProperty("zodiaque", c.getSigne_z());
                            jsonClient.addProperty("animal_totem", c.getTotem());
                            jsonClient.addProperty("date_Naiss", sf.format(c.getDateNaissance()));
                            container.add("client",jsonClient);
                            for(String[] s:Service.historiqueClient(c,true)){
                                JsonObject jsonHistorique = new JsonObject();
                                jsonHistorique.addProperty("medium",s[0]);
                                jsonHistorique.addProperty("employe",s[1]);
                                jsonHistorique.addProperty("date",s[2]);
                                jsonHistorique.addProperty("heure_debut",s[3]);
                                jsonHistorique.addProperty("heure_fin",s[4]);
                                jsonHistorique.addProperty("commentaire",s[5]);
                                jsonListeHistorique.add(jsonHistorique);
                            }
                            container.add("liste_historique",jsonListeHistorique);
                        }
                        else{
                            container.addProperty("status", false);
                        }
                    }
                    out.println(gson.toJson(container));
                }
            }
            else if ("commencer-voyance".equals(action)) {
                try(PrintWriter out = response.getWriter()){
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    JsonObject container = new JsonObject(); 
                    Employe e = (Employe)session.getAttribute("employe");

                    if(e != null){
                        e = Service.commencerVoyance(e, new Date());
                        session.setAttribute("employe",e);
                        container.addProperty("status",true);
                    }
                    else{
                        container.addProperty("status",false);
                    }
                    out.println(gson.toJson(container));
                } 
            }
            else if("initialiser-voyance-employe".equals(action)){
                Employe e = (Employe)session.getAttribute("employe");
                
                try(PrintWriter out = response.getWriter()){
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    JsonObject container = new JsonObject();
                    if(e == null){
                        container.addProperty("connexion", false);
                    }
                    else{
                        container.addProperty("connexion", true);
                        Client c = (Client)session.getAttribute("ClientEnAttente");
                        if( c != null){
                            JsonObject jsonClient = new JsonObject();
                            container.addProperty("status",true);       
                            jsonClient.addProperty("civilite", c.getCivilite());
                            jsonClient.addProperty("nom", c.getNom());
                            jsonClient.addProperty("prenom", c.getPrenom());
                            jsonClient.addProperty("adresse", c.getAddresse());
                            jsonClient.addProperty("mail", c.getMail());
                            jsonClient.addProperty("tel", c.getTel());
                            jsonClient.addProperty("signe_chinois", c.getSigne_ch());
                            jsonClient.addProperty("coul_fav", c.getCouleur());
                            jsonClient.addProperty("zodiaque", c.getSigne_z());
                            jsonClient.addProperty("animal_totem", c.getTotem());
                            jsonClient.addProperty("date_Naiss", sf.format(c.getDateNaissance()));
                            container.add("client",jsonClient);
                        }
                        else{
                            container.addProperty("status", false);
                        }
                    }
                    out.println(gson.toJson(container));
                }
            }
            else if ("realiser-prediction".equals(action)) {
                Employe e = (Employe)session.getAttribute("employe");
                try(PrintWriter out = response.getWriter()){
                    int amour = Integer.parseInt(request.getParameter("amour"));
                    int travail= Integer.parseInt(request.getParameter("travail"));
                    int sante = Integer.parseInt(request.getParameter("sante"));
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    JsonObject container = new JsonObject(); 
                    if(e != null){
                        container.addProperty("connexion",true);
                        Client c = (Client)  session.getAttribute("ClientEnAttente");
                        if(c!= null){
                            container.addProperty("status",true);
                            JsonArray jsonListePrediction = new JsonArray();
                            for(String s:Service.prediction(c,amour,travail,sante)){
                                JsonObject jsonPrediction = new JsonObject();
                                jsonPrediction.addProperty("prediction",s);
                                jsonListePrediction.add(jsonPrediction);
                            }
                            container.add("liste_predictions",jsonListePrediction);
                        }
                        else{
                            container.addProperty("status",false);
                        }
                    }
                    else{
                        container.addProperty("connexion",false);
                    }
                    out.println(gson.toJson(container));
                } 
            }
            else if ("terminer-consultation".equals(action)) {
                Employe e = (Employe)session.getAttribute("employe");
                try(PrintWriter out = response.getWriter()){
                    String commentaire = request.getParameter("commentaire");
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    JsonObject container = new JsonObject(); 
                    if(e != null){
                        container.addProperty("connexion",true);
                        Service.terminerVoyance(e, commentaire, new Date());
                    }
                    else{
                        container.addProperty("connexion",false);
                    }
                    out.println(gson.toJson(container));
                } 
            }
            else if ("initialiser-statistique".equals(action)) {
                Employe e = (Employe)session.getAttribute("employe");
                try(PrintWriter out = response.getWriter()){
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    JsonObject container = new JsonObject(); 
                    JsonArray jsonVoyancesParEmploye = new JsonArray();
                    JsonArray jsonVoyancesParMedium = new JsonArray();
                    JsonArray jsonRepartition = new JsonArray();
                    if(e != null){
                        container.addProperty("connexion",true);
                        HashMap<String, Long> nbVoyanceParMedium = Service.nbVoyancesParMedium();
                        for(String s : nbVoyanceParMedium.keySet()){
                            JsonObject jsonElement = new JsonObject();
                            jsonElement.addProperty("nom",s);
                            jsonElement.addProperty("valeur",nbVoyanceParMedium.get(s));
                            jsonVoyancesParMedium.add(jsonElement);
                        }
                        container.add("voyances_par_medium",jsonVoyancesParMedium);
                        HashMap<String, Long> nbVoyanceParEmp = Service.nbVoyancesParEmploye();
                        for(String s : nbVoyanceParEmp.keySet()){
                            JsonObject jsonElement = new JsonObject();
                            jsonElement.addProperty("nom",s);
                            jsonElement.addProperty("valeur",nbVoyanceParEmp.get(s));
                            jsonVoyancesParEmploye.add(jsonElement);
                        }
                        container.add("voyances_par_employe",jsonVoyancesParEmploye);
                        HashMap<String, Double> repartition = Service.repartitionVoyancesEmployes();
                        for(String s : repartition.keySet()){
                            JsonObject jsonElement = new JsonObject();
                            jsonElement.addProperty("name",s);
                            jsonElement.addProperty("y",repartition.get(s));
                            jsonRepartition.add(jsonElement);
                        }
                        container.add("repartition",jsonRepartition);
                    }
                    else{
                        container.addProperty("connexion",false);
                    }
                    out.println(gson.toJson(container));
                } 
            }
        }
        
    }

   /* @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
       processRequest(req,resp);
    }*/

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
