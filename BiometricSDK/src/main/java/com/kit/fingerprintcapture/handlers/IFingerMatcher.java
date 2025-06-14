package com.kit.fingerprintcapture.handlers;

import com.dermalog.afis.fingercode3.TemplateFormat;
import com.kit.fingerprintcapture.model.FingerprintID;
import com.kit.fingerprintcapture.template.ISOTemplate;
import com.kit.fingerprintcapture.template.MatchResult;

import java.util.List;

public interface IFingerMatcher {


    // Used by Morpho
    long verifyFingerPrint(FingerprintID nowID, byte[] nowImage, int nowWidth, int nowHeight, boolean[] matched);

    // Used by Dermalog
    void verifyFingerPrint(Integer fingerprintId, ISOTemplate searchTemplate,
                           List<ISOTemplate> referenceTemplateList,
                           List<MatchResult> result,
                           TemplateFormat subjectTmplType,
                           TemplateFormat candidateTmplType);
}
