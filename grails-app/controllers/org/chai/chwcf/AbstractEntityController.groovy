/**
 * Copyright (c) 2011, Clinton Health Access Initiative.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.chai.chwcf;


import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.shiro.SecurityUtils;
import org.hisp.dhis.dataset.DataSet;
import org.chai.chwcf.organisation.Organisation;
import org.chai.chwcf.organisation.OrganisationService;
import org.chai.chwcf.security.User;
/**
 * @author Jean Kahigiso M.
 *
 */
abstract class AbstractEntityController {
	
	def localeService
	OrganisationService organisationService
	
	def getTargetURI() {
		return params.targetURI?: "/"
	}
	
	def index = {
        redirect(action: "list", params: params)
    }
	
	protected def getUser() {
		return User.findByUsername(SecurityUtils.subject.principal)
	}
	
	protected def getOrganisation(def defaultIfNull) {
		
		Organisation organisation = null;
		if (params.int('organisation')){
			 organisation = organisationService.getOrganisation(params.int('organisation'));
		}
		//if true, return the root organisation
		//if false, don't return the root organisation
		if (organisation == null && defaultIfNull) {
			organisation = organisationService.getRootOrganisation();
		}
		return organisation
	}

	def delete = {		
		def entity = getEntity(params.int('id'));
		
		if (entity != null) {
			try {
				deleteEntity(entity)
				
				if (!flash.message) flash.message = message(code: 'default.deleted.message', args: [message(code: getLabel(), default: 'entity'), params.id])
				redirect(uri: getTargetURI())
			}
			catch (org.springframework.dao.DataIntegrityViolationException e) {
				flash.message = message(code: 'default.not.deleted.message', args: [message(code: getLabel(), default: 'entity'), params.id])
				redirect(uri: getTargetURI())
			}
		}
		else {
			flash.message = message(code: 'default.not.found.message', args: [message(code: getLabel(), default: 'entity'), params.id])
			redirect(uri: getTargetURI())
		}
	}
	
	def edit = {
		def entity = getEntity(params.int('id'));

		if (entity == null) {
			flash.message = message(code: 'default.not.found.message', args: [message(code: getLabel(), default: 'entity'), params.id])
			redirect(uri: getTargetURI())
		}
		else {
			def model = getModel(entity)
			model << [template: getTemplate()]
			model << [targetURI: getTargetURI()]
			render(view: '/admin/edit', model: model)
		}
	}
	
	def create = {
		def entity = createEntity()
		bindParams(entity);
		
		def model = getModel(entity)
		model << [template: getTemplate()]
		model << [targetURI: getTargetURI()]
		render(view: '/admin/edit', model: model)
	}
	
	def save = {
		withForm {
			saveWithoutTokenCheck()
		}.invalidToken {
			log.warn("clicked twice");
		}
	}
	
	def saveWithoutTokenCheck = {
		
		def entity = getEntity(params.int('id'));
		if (entity == null) {
			entity = createEntity()
		}
		bindParams(entity)
		log.debug('bound params, entity: '+entity)
		if (!validateEntity(entity)) {
			log.info ("validation error in ${entity}: ${entity.errors}}")
			
			def model = getModel(entity)
			model << [template: getTemplate()]
			model << [targetURI: getTargetURI()]
			render(view: "/admin/edit", model: model)
		}
		else {
			saveEntity(entity);
			
			flash.message = message(code: 'default.saved.message', args: [message(code: getLabel(), default: 'entity'), params.id])
			redirect(url: getTargetURI())
		}
	}
	
	def validateEntity(def entity) {
		return entity.validate()
	}

	def saveEntity(def entity) {
		entity.save()
	}

	def deleteEntity(def entity) {
		entity.delete()
	}
	
	protected abstract def bindParams(def entity);
	
	protected abstract def getModel(def entity);
		
	protected abstract def getEntity(def id);
	
	protected abstract def createEntity();
	
	protected abstract def getTemplate();
	
	protected abstract def getLabel();
}