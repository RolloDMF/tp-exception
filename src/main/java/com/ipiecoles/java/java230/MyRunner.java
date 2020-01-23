package com.ipiecoles.java.java230;

import com.ipiecoles.java.java230.exceptions.BatchException;
import com.ipiecoles.java.java230.exceptions.TechnicienException;
import com.ipiecoles.java.java230.model.Commercial;
import com.ipiecoles.java.java230.model.Employe;
import com.ipiecoles.java.java230.model.Manager;
import com.ipiecoles.java.java230.model.Technicien;
import com.ipiecoles.java.java230.repository.EmployeRepository;
import com.ipiecoles.java.java230.repository.ManagerRepository;

import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class MyRunner implements CommandLineRunner {

    private static final String REGEX_MATRICULE = "^[MTC][0-9]{5}$";
    private static final String REGEX_NOM = ".*";
    private static final String REGEX_PRENOM = ".*";
    private static final int NB_CHAMPS_MANAGER = 5;
    private static final int NB_CHAMPS_TECHNICIEN = 7;
    private static final String REGEX_MATRICULE_MANAGER = "^M[0-9]{5}$";
    private static final int NB_CHAMPS_COMMERCIAL = 7;
    private static List<String> managers = new ArrayList<String>();;

    @Autowired
    private EmployeRepository employeRepository;

    @Autowired
    private ManagerRepository managerRepository;

    private List<Employe> employes = new ArrayList<Employe>();

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void run(String... strings) throws Exception {
        String fileName = "employes.csv";
        readFile(fileName);
    }

    /**
     * Méthode qui lit le fichier CSV en paramètre afin d'intégrer son contenu en BDD
     * @param fileName Le nom du fichier (à mettre dans src/main/resources)
     * @return une liste contenant les employés à insérer en BDD ou null si le fichier n'a pas pu être le
     */
    public List<Employe> readFile(String fileName) throws Exception {
        Stream<String> stream;
        stream = Files.lines(Paths.get(new ClassPathResource(fileName).getURI()));
        Integer i = 0;

        for(String ligne : stream.collect(Collectors.toList())){
            i++;
            try {
                processLine(ligne);
            } catch (BatchException b) {
                logger.error("Ligne " + i + " : " + b.getMessage() + " => " + ligne);
            }
        }

        return employes;
    }

    /**
     * Méthode qui regarde le premier caractère de la ligne et appelle la bonne méthode de création d'employé
     * @param ligne la ligne à analyser
     * @throws BatchException si le type d'employé n'a pas été reconnu
     */
    private void processLine(String ligne) throws BatchException {
        String[] splitedLine = ligne.split(",");
        
        if (!ligne.matches("^[MCT]{1}.*")) {
            throw new BatchException("Type d'employé inconnu : " + ligne.charAt(0) + " => " + ligne);
        } else if (!splitedLine[0].matches(REGEX_MATRICULE)) {
            throw new BatchException("la chaîne " + splitedLine[0] + " ne respecte pas l'expression régulière ^[MTC][0-9]{5}$");
        } else if (!splitedLine[1].matches(REGEX_NOM) || !splitedLine[2].matches(REGEX_PRENOM)) {
            throw new BatchException("Le nom ou le prenom de la ligne " + ligne + " ne respecte pas l'expression régulière .*");
        } else {
            switch (ligne.substring(0,1)) {
                case "M":
                    processManager(ligne);
                break;

                case "C":                  
                    processCommercial(ligne);
                break;

                case "T":
                    processTechnicien(ligne);
                break;
            }
        }

        try {
            Double.parseDouble(splitedLine[4]);
        } catch (Exception e) {
            throw new BatchException(splitedLine[4] + " n'est pas un nombre valide pour un salaire");
        }
    }

    /**
     * Méthode qui crée un Commercial à partir d'une ligne contenant les informations d'un commercial et l'ajoute dans la liste globale des employés
     * @param ligneCommercial la ligne contenant les infos du commercial à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processCommercial(String ligneCommercial) throws BatchException {
        Commercial c = new Commercial();

        String[] splitedLine = ligneCommercial.split(",");

        if (splitedLine.length != NB_CHAMPS_COMMERCIAL) {
            throw new BatchException("La ligne technicien ne contient pas " + NB_CHAMPS_COMMERCIAL + " éléments mais " + splitedLine.length);
        }

        try {
            c.setDateEmbauche(DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(splitedLine[3])); 
         } catch (Exception e) {
             throw new BatchException(splitedLine[3] + " ne respecte pas le format de date dd/MM/yyyy");
         }

        try {
            Double.parseDouble(splitedLine[5]);
        } catch (Exception e) {
            throw new BatchException("Le chiffre d'affaire du commercial est incorrect : " + splitedLine[5]);
        }

        try {
            Integer.parseInt(splitedLine[6]);
        } catch (Exception e) {
            throw new BatchException("La performance du commercial est incorrecte : " + splitedLine[6]);
        }

        c.setCaAnnuel(Double.parseDouble(splitedLine[5]));
        c.setMatricule(splitedLine[0]);
        c.setNom(splitedLine[1]);
        c.setPerformance(Integer.parseInt(splitedLine[6]));
        c.setPrenom(splitedLine[2]);
        c.setSalaire(Double.parseDouble(splitedLine[4]));

        //employeRepository.save(c);
    }

    /**
     * Méthode qui crée un Manager à partir d'une ligne contenant les informations d'un manager et l'ajoute dans la liste globale des employés
     * @param ligneManager la ligne contenant les infos du manager à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processManager(String ligneManager) throws BatchException {
        Manager m = new Manager();

        String[] splitedLine = ligneManager.split(",");
        
        managers.add(splitedLine[0]);
        
        if (splitedLine.length != NB_CHAMPS_MANAGER) {
            throw new BatchException("La ligne manager ne contient pas " + NB_CHAMPS_MANAGER + " éléments mais " + splitedLine.length);
        }

        try {
            m.setDateEmbauche(DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(splitedLine[3])); 
         } catch (Exception e) {
             throw new BatchException(splitedLine[3] + " ne respecte pas le format de date dd/MM/yyyy");
         }

         m.setMatricule(splitedLine[0]);
         m.setNom(splitedLine[1]);
         m.setPrenom(splitedLine[2]);
         m.setSalaire(Double.parseDouble(splitedLine[4]));

        // employeRepository.save(m);
    }

    /**
     * Méthode qui crée un Technicien à partir d'une ligne contenant les informations d'un technicien et l'ajoute dans la liste globale des employés
     * @param ligneTechnicien la ligne contenant les infos du technicien à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processTechnicien(String ligneTechnicien) throws BatchException {
        Technicien t = new Technicien();

        String[] splitedLine = ligneTechnicien.split(",");
        Boolean findManager = false;

        if (splitedLine.length != NB_CHAMPS_TECHNICIEN) {
            throw new BatchException("La ligne technicien ne contient pas " + NB_CHAMPS_TECHNICIEN + " éléments mais " + splitedLine.length);
        }

        if (!splitedLine[6].matches(REGEX_MATRICULE_MANAGER)) {
            throw new BatchException("la chaîne " + splitedLine[6] + " ne respecte pas l'expression régulière ^M[0-9]{5}$");
        }

        //si le manager est trouvé dans le fichier , on passe la variable findManager a true
        for (String manager : managers) {
            if (manager == splitedLine[6]) {
                findManager = true;
            }
        }

        Manager manager = managerRepository.findByMatricule(splitedLine[6]);

        //si le manager n est pas en bdd et n est pas dans le tableau , alors on lance une exception
        if (manager == null && !findManager) {
            throw new BatchException("Le manager de matricule " + splitedLine[6] + " n'a pas été trouvé dans le fichier ou en base de données");
        }else{
            t.setManager(manager);
        };

        try {
            t.setDateEmbauche(DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(splitedLine[3])); 
         } catch (Exception e) {
             throw new BatchException(splitedLine[3] + " ne respecte pas le format de date dd/MM/yyyy");
         }

        try {
            t.setGrade(Integer.parseInt(splitedLine[5]));
        } catch (TechnicienException e) {
            throw new BatchException(e.getMessage());
        } catch (Exception e) {
            throw new BatchException("Le grade du technicien est incorrect : " + splitedLine[5]);
        }


        t.setMatricule(splitedLine[0]);
        t.setNom(splitedLine[1]);
        t.setPrenom(splitedLine[2]);
        t.setSalaire(Double.parseDouble(splitedLine[4]));

        //employeRepository.save(t);
    }

}
