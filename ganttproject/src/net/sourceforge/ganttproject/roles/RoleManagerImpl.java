/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.roles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.language.GanttLanguage.Event;

/**
 * @author athomas
 */
public class RoleManagerImpl implements RoleManager {
  private final List<Listener> myListeners = new ArrayList<Listener>();

  private final RoleSetImpl myProjectRoleSet = new RoleSetImpl(null, this);

  private final ArrayList<RoleSet> myRoleSets = new ArrayList<RoleSet>();

  private final RoleSetImpl SOFTWARE_DEVELOPMENT_ROLE_SET;
  private final RoleSetImpl DEFAULT_ROLE_SET;

  public RoleManagerImpl() {
    DEFAULT_ROLE_SET = new RoleSetImpl(RoleSet.DEFAULT, this);
    SOFTWARE_DEVELOPMENT_ROLE_SET = new RoleSetImpl(RoleSet.SOFTWARE_DEVELOPMENT, this);
    createRoleSet();
    clear();
    myRoleSets.add(DEFAULT_ROLE_SET);
    myRoleSets.add(SOFTWARE_DEVELOPMENT_ROLE_SET);
    myProjectRoleSet.setEnabled(true);
    SOFTWARE_DEVELOPMENT_ROLE_SET.setEnabled(false);

    GanttLanguage.getInstance().addListener(new GanttLanguage.Listener() {
      @Override
      public void languageChanged(Event event) {
        changeRoleSet();
      }
    });
  }

  @Override
  public void clear() {
    myProjectRoleSet.clear();
    for (int i = 0; i < myRoleSets.size(); i++) {
      RoleSet next = myRoleSets.get(i);
      next.setEnabled(false);
    }
    myProjectRoleSet.setEnabled(true);
    DEFAULT_ROLE_SET.setEnabled(true);
    SOFTWARE_DEVELOPMENT_ROLE_SET.setEnabled(false);
  }

  @Override
  public Role[] getProjectLevelRoles() {
    return myProjectRoleSet.getRoles();
  }

  @Override
  public RoleSet[] getRoleSets() {
    return myRoleSets.toArray(new RoleSet[0]);
  }

  @Override
  public RoleSet createRoleSet(String name) {
    RoleSet result = new RoleSetImpl(name, this);
    myRoleSets.add(result);
    // System.err.println("[RoleManagerImpl] createRoleSet():
    // created:"+name);
    return result;
  }

  @Override
  public RoleSet getProjectRoleSet() {
    return myProjectRoleSet;
  }

  @Override
  public RoleSet getRoleSet(String rolesetName) {
    RoleSet result = null;
    RoleSet[] roleSets = getRoleSets();
    for (int i = 0; i < roleSets.length; i++) {
      if (roleSets[i].getName().equals(rolesetName)) {
        result = roleSets[i];
        break;
      }
    }
    return result;
  }

  @Override
  public Role[] getEnabledRoles() {
    ArrayList<Role> result = new ArrayList<Role>();
    RoleSet[] roleSets = getRoleSets();
    for (int i = 0; i < roleSets.length; i++) {
      if (roleSets[i].isEnabled()) {
        result.addAll(Arrays.asList(roleSets[i].getRoles()));
      }
    }
    result.addAll(Arrays.asList(getProjectRoleSet().getRoles()));
    return result.toArray(new Role[0]);
  }

  @Override
  public Role getDefaultRole() {
    return DEFAULT_ROLE_SET.findRole(0);
  }

  @Override
  public Role getRole(String persistentID) {
    Role[] enabledRoles = getEnabledRoles();
    for (int i = 0; i < enabledRoles.length; i++) {
      if (enabledRoles[i].getPersistentID().equalsIgnoreCase(persistentID)) {
        return enabledRoles[i];
      }
    }
    return null;
  }

  @Override
  public void importData(RoleManager original) {
    myProjectRoleSet.importData(original.getProjectRoleSet());
    RoleSet[] originalRoleSets = original.getRoleSets();
    HashSet<String> thisNames = new HashSet<String>();
    for (int i = 0; i < myRoleSets.size(); i++) {
      RoleSet next = myRoleSets.get(i);
      thisNames.add(next.getName());
    }
    for (int i = 0; i < originalRoleSets.length; i++) {
      RoleSet next = originalRoleSets[i];
      if (!thisNames.contains(next.getName())) {
        myRoleSets.add(next);
      }
    }
    // myRoleSets.addAll(Arrays.asList(originalRoleSets));
  }

  @Override
  public void addRoleListener(Listener listener) {
    myListeners.add(listener);
  }

  @Override
  public void removeRoleListener(Listener listener) {
    myListeners.remove(listener);
  }

  void fireRolesChanged(RoleSet changedRoleSet) {
    RoleEvent event = new RoleEvent(this, changedRoleSet);
    for (int i = 0; i < myListeners.size(); i++) {
      Listener next = myListeners.get(i);
      next.rolesChanged(event);
    }
  }

  // Adiciona um role novo a lista de roles existentes.
  public void addRole(String name) {
    GanttLanguage language = GanttLanguage.getInstance();
    boolean roleExists = false;
    Role[] r1 = SOFTWARE_DEVELOPMENT_ROLE_SET.getRoles();
    Role[] r2 = DEFAULT_ROLE_SET.getRoles();
    int currlen = 0;
    
    //cleanRolesCache(r1);

    for (int i = 0; i < r1.length; i++) {
      if (r1[i].getName().equals(name)) {
        roleExists = true;
      }
    }

    for (int i = 0; i < r1.length; i++) {
      deleteDuplicated(r1[i].getName(), r1);
    }
    
    for (int i = 0; i < r1.length; i++) {
      if (r1[i] != null) {
        if (i > currlen) {
          currlen = i;
        }
      }
    }

    if (!roleExists) {
      SOFTWARE_DEVELOPMENT_ROLE_SET.createRole(name);
      
      int temp = 0;
      Role[] r1s = SOFTWARE_DEVELOPMENT_ROLE_SET.getRoles();
      for (int i = 0; i < r1s.length; i++) {
        if (r1s[i].getName().equals(name)) {
          temp = r1s[i].getID();
        }
      }

      System.err.println("Ultimo elemento : " + SOFTWARE_DEVELOPMENT_ROLE_SET.findRole(temp));

      SOFTWARE_DEVELOPMENT_ROLE_SET.changeRole(name, temp-1);
      SOFTWARE_DEVELOPMENT_ROLE_SET.changeRole(language.getText("customRole"), temp);
    }
    
    debugPrints(r1, r2);

  }

  private void deleteDuplicated(String name, Role[] r1) {
    int max = 0;
    for (int i = 0; i < r1.length; i++) {
      if (r1[i].getName().equals(name)) {
        if (i > max) {
          max = i;
        }
      }
    }

    for (int i = 0; i < r1.length; i++) {
      if (r1[i].getName().equals(name)) {
        if (i < max) {
          SOFTWARE_DEVELOPMENT_ROLE_SET.deleteRole(r1[i]);
        }
      }
    }
  }

  private void debugPrints(Role[] r1, Role[] r2) {
    System.err.println("Aqui comeca a cache.");

    for (int i = 0; i < r1.length; i++) {
      System.err.println("SRole ["+i+"] : " + r1[i].getName());
    }
    for (int i = 0; i < r2.length; i++) {
      System.err.println("Role ["+i+"] : " + r2[i].getName());
    }
  }

  public void cleanRolesCache(Role[] r1) {
    for (int i = 0; i < r1.length; i++) {
      SOFTWARE_DEVELOPMENT_ROLE_SET.deleteRole(r1[i]);
    }

    createRoleSet();
  }

  public void createRoleSet() {
    GanttLanguage language = GanttLanguage.getInstance();
    Role[] rs = SOFTWARE_DEVELOPMENT_ROLE_SET.getRoles();
    boolean temp = false;

    SOFTWARE_DEVELOPMENT_ROLE_SET.clear();
    SOFTWARE_DEVELOPMENT_ROLE_SET.createRole(language.getText("resDeveloper"), 2);
    SOFTWARE_DEVELOPMENT_ROLE_SET.createRole(language.getText("resDocWriter"), 3);
    SOFTWARE_DEVELOPMENT_ROLE_SET.createRole(language.getText("resTester"), 4);
    SOFTWARE_DEVELOPMENT_ROLE_SET.createRole(language.getText("resGraphicDesigner"), 5);
    SOFTWARE_DEVELOPMENT_ROLE_SET.createRole(language.getText("resDocTranslator"), 6);
    SOFTWARE_DEVELOPMENT_ROLE_SET.createRole(language.getText("resPackager"), 7);
    SOFTWARE_DEVELOPMENT_ROLE_SET.createRole(language.getText("resAnalysis"), 8);
    SOFTWARE_DEVELOPMENT_ROLE_SET.createRole(language.getText("resWebDesigner"), 9);
    SOFTWARE_DEVELOPMENT_ROLE_SET.createRole(language.getText("resNoSpecificRole"), 10);
    SOFTWARE_DEVELOPMENT_ROLE_SET.createRole(language.getText("customRole"), 11);
    DEFAULT_ROLE_SET.clear();
    DEFAULT_ROLE_SET.createRole(language.getText("resUndefined"), 0);
    DEFAULT_ROLE_SET.createRole(language.getText("resProjectManager"), 1);
    DEFAULT_ROLE_SET.setEnabled(true);
  }

  private void changeRoleSet() {
    GanttLanguage language = GanttLanguage.getInstance();

    SOFTWARE_DEVELOPMENT_ROLE_SET.changeRole(language.getText("resDeveloper"), 2);
    SOFTWARE_DEVELOPMENT_ROLE_SET.changeRole(language.getText("resDocWriter"), 3);
    SOFTWARE_DEVELOPMENT_ROLE_SET.changeRole(language.getText("resTester"), 4);
    SOFTWARE_DEVELOPMENT_ROLE_SET.changeRole(language.getText("resGraphicDesigner"), 5);
    SOFTWARE_DEVELOPMENT_ROLE_SET.changeRole(language.getText("resDocTranslator"), 6);
    SOFTWARE_DEVELOPMENT_ROLE_SET.changeRole(language.getText("resPackager"), 7);
    SOFTWARE_DEVELOPMENT_ROLE_SET.changeRole(language.getText("resAnalysis"), 8);
    SOFTWARE_DEVELOPMENT_ROLE_SET.changeRole(language.getText("resWebDesigner"), 9);
    SOFTWARE_DEVELOPMENT_ROLE_SET.changeRole(language.getText("resNoSpecificRole"), 10);
    //SOFTWARE_DEVELOPMENT_ROLE_SET.changeRole(language.getText("customRole"), 11);
    DEFAULT_ROLE_SET.changeRole(language.getText("resUndefined"), 0);
    DEFAULT_ROLE_SET.changeRole(language.getText("resProjectManager"), 1);
  }
}
