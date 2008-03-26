package org.mulgara.resolver;

// Java 2 standard packages

// Java 2 enterprise packages

// Third party packages
import org.apache.log4j.Logger;

// Local packages
import org.mulgara.query.rdf.URIReferenceImpl;
import org.mulgara.resolver.spi.DatabaseMetadata;
import org.mulgara.resolver.spi.ResolverSessionFactory;
import org.mulgara.resolver.spi.SingletonStatements;
import org.mulgara.resolver.spi.SystemResolver;

class BootstrapOperation implements Operation
{
  /** Logger */
  private static final Logger logger =
    Logger.getLogger(BootstrapOperation.class.getName());

  /**
   * The URI of the model to be created.
   */
  private final DatabaseMetadataImpl databaseMetadata;

  private long result;

  BootstrapOperation(DatabaseMetadataImpl databaseMetadata) {
    if (databaseMetadata == null) {
      throw new IllegalArgumentException("BootstrapSystemModel - databaseMetadata null ");
    }

    this.databaseMetadata = databaseMetadata;
    this.result = -1; // Return invalid node by default.
  }

  public void execute(OperationContext       operationContext,
                      SystemResolver         systemResolver,
                      DatabaseMetadata       metadata) throws Exception {
    // Find the local node identifying the model
    long model = systemResolver.localizePersistent(
        new URIReferenceImpl(databaseMetadata.getSystemModelURI()));
    long rdfType = systemResolver.localizePersistent(
        new URIReferenceImpl(databaseMetadata.getRdfTypeURI()));
    long modelType = systemResolver.localizePersistent(
        new URIReferenceImpl(databaseMetadata.getSystemModelTypeURI()));

    // Use the session to create the model
    systemResolver.modifyModel(model, new SingletonStatements(model, rdfType,
        modelType), true);
    databaseMetadata.initializeSystemNodes(model, rdfType, modelType);

    long preSubject = systemResolver.localizePersistent(
        new URIReferenceImpl(databaseMetadata.getPreallocationSubjectURI()));
    long prePredicate = systemResolver.localizePersistent(
        new URIReferenceImpl(databaseMetadata.getPreallocationPredicateURI()));
    long preModel = systemResolver.localizePersistent(
        new URIReferenceImpl(databaseMetadata.getPreallocationModelURI()));

    // Every node cached by DatabaseMetadata must be preallocated
    systemResolver.modifyModel(preModel,
        new SingletonStatements(preSubject, prePredicate, model),
        true);
    systemResolver.modifyModel(preModel,
        new SingletonStatements(preSubject, prePredicate, rdfType),
        true);
    systemResolver.modifyModel(preModel,
        new SingletonStatements(preSubject, prePredicate, modelType),
        true);
    systemResolver.modifyModel(preModel,
        new SingletonStatements(preSubject, prePredicate, preSubject),
        true);
    systemResolver.modifyModel(preModel,
        new SingletonStatements(preSubject, prePredicate, prePredicate),
        true);
    systemResolver.modifyModel(preModel,
        new SingletonStatements(preSubject, prePredicate, preModel),
        true);

    databaseMetadata.initializePreallocationNodes(preSubject, prePredicate, preModel);

    result = model;
  }

  public boolean isWriteOperation()
  {
    return true;
  }

  public long getResult() {
    return result;
  }
}
