package io.sls.persistence.impl.regulardictionary.rest;

import io.sls.persistence.IResourceStore;
import io.sls.persistence.impl.resources.rest.RestVersionInfo;
import io.sls.resources.rest.documentdescriptor.IDocumentDescriptorStore;
import io.sls.resources.rest.documentdescriptor.model.DocumentDescriptor;
import io.sls.resources.rest.patch.PatchInstruction;
import io.sls.resources.rest.regulardictionary.IRegularDictionaryStore;
import io.sls.resources.rest.regulardictionary.IRestRegularDictionaryStore;
import io.sls.resources.rest.regulardictionary.model.RegularDictionaryConfiguration;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

/**
 * @author ginccc
 */
@Slf4j
public class RestRegularDictionaryStore extends RestVersionInfo<RegularDictionaryConfiguration> implements IRestRegularDictionaryStore {
    private final IRegularDictionaryStore regularDictionaryStore;
    private final IDocumentDescriptorStore documentDescriptorStore;

    @Inject
    public RestRegularDictionaryStore(IRegularDictionaryStore regularDictionaryStore,
                                      IDocumentDescriptorStore documentDescriptorStore) {
        super(resourceURI, regularDictionaryStore);
        this.regularDictionaryStore = regularDictionaryStore;
        this.documentDescriptorStore = documentDescriptorStore;
    }

    @Override
    public List<DocumentDescriptor> readRegularDictionaryDescriptors(String filter, Integer index, Integer limit) {
        try {
            return documentDescriptorStore.readDescriptors("io.sls.regulardictionary", filter, index, limit, false);
        } catch (IResourceStore.ResourceStoreException e) {
            log.error(e.getLocalizedMessage(), e);
            throw new InternalServerErrorException(e.getLocalizedMessage(), e);
        } catch (IResourceStore.ResourceNotFoundException e) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }

    @Override
    public RegularDictionaryConfiguration readRegularDictionary(String id, Integer version, String filter, String order, Integer index, Integer limit) {
        return read(id, version);
    }

    @Override
    public List<String> readExpressions(String id, Integer version, String filter, String order, Integer index, Integer limit) {
        try {
            return regularDictionaryStore.readExpressions(id, version, filter, order, index, limit);
        } catch (IResourceStore.ResourceStoreException e) {
            log.error(e.getLocalizedMessage(), e);
            throw new InternalServerErrorException(e.getLocalizedMessage(), e);
        } catch (IResourceStore.ResourceNotFoundException e) {
            throw new NotFoundException(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public URI updateRegularDictionary(String id, Integer version, RegularDictionaryConfiguration regularDictionaryConfiguration) {
        return update(id, version, regularDictionaryConfiguration);
    }

    @Override
    public Response createRegularDictionary(RegularDictionaryConfiguration regularDictionaryConfiguration) {
        return create(regularDictionaryConfiguration);
    }

    @Override
    public void deleteRegularDictionary(String id, Integer version) {
        delete(id, version);
    }

    @Override
    public URI patchRegularDictionary(String id, Integer version, PatchInstruction<RegularDictionaryConfiguration>[] patchInstructions) {
        try {
            RegularDictionaryConfiguration currentRegularDictionaryConfiguration = regularDictionaryStore.read(id, version);
            RegularDictionaryConfiguration patchedRegularDictionaryConfiguration = patchDocument(currentRegularDictionaryConfiguration, patchInstructions);

            return updateRegularDictionary(id, version, patchedRegularDictionaryConfiguration);

        } catch (IResourceStore.ResourceStoreException e) {
            log.error(e.getLocalizedMessage(), e);
            throw new InternalServerErrorException(e.getLocalizedMessage(), e);
        } catch (IResourceStore.ResourceNotFoundException e) {
            throw new NotFoundException(e.getLocalizedMessage(), e);
        }
    }

    private RegularDictionaryConfiguration patchDocument(RegularDictionaryConfiguration currentRegularDictionaryConfiguration, PatchInstruction<RegularDictionaryConfiguration>[] patchInstructions) throws IResourceStore.ResourceStoreException {
        for (PatchInstruction<RegularDictionaryConfiguration> patchInstruction : patchInstructions) {
            RegularDictionaryConfiguration regularDictionaryConfigurationPatch = patchInstruction.getDocument();
            switch (patchInstruction.getOperation()) {
                case SET:
                    if (regularDictionaryConfigurationPatch.getLanguage() != null) {
                        currentRegularDictionaryConfiguration.setLanguage(regularDictionaryConfigurationPatch.getLanguage());
                    }
                    currentRegularDictionaryConfiguration.getWords().removeAll(regularDictionaryConfigurationPatch.getWords());
                    currentRegularDictionaryConfiguration.getWords().addAll(regularDictionaryConfigurationPatch.getWords());
                    currentRegularDictionaryConfiguration.getPhrases().removeAll(regularDictionaryConfigurationPatch.getPhrases());
                    currentRegularDictionaryConfiguration.getPhrases().addAll(regularDictionaryConfigurationPatch.getPhrases());
                    break;
                case DELETE:
                    currentRegularDictionaryConfiguration.getWords().removeAll(regularDictionaryConfigurationPatch.getWords());
                    currentRegularDictionaryConfiguration.getPhrases().removeAll(regularDictionaryConfigurationPatch.getPhrases());
                    break;
                default:
                    throw new IResourceStore.ResourceStoreException("Patch operation must be either SET or DELETE!");
            }
        }

        return currentRegularDictionaryConfiguration;
    }

    @Override
    protected IResourceStore.IResourceId getCurrentResourceId(String id) throws IResourceStore.ResourceNotFoundException {
        return regularDictionaryStore.getCurrentResourceId(id);
    }
}
