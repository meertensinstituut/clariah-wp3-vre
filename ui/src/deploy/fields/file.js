import React from "react";
import {FormControl, FormGroup, Well} from 'react-bootstrap';
import RemoveButton from "./remove-button";

export default class File extends React.Component {

    static handleValidation(value) {
        const length = value.length;
        if (length === 0) return null;
        return 'success';
    }

    render() {
        return (
            <FormGroup
                controlId="formBasicText"
                validationState={File.handleValidation(this.props.value)}
            >
                <div className="input-group mb-3">
                    <FormControl
                        type="text"
                        disabled
                        value={this.props.value}
                        className="d-none"
                    />
                    <Well className="w-100">Selected file: {this.props.param.fileData.filepath}</Well>
                    <FormControl.Feedback/>
                    <div className="input-group-append">
                        <RemoveButton
                            canRemove={this.props.canRemove}
                            onRemove={this.props.onRemove}
                            nextToInput={true}
                        />
                    </div>
                </div>
            </FormGroup>
        );
    }
}