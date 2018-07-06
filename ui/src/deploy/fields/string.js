import React from "react";
import {FormControl, FormGroup} from 'react-bootstrap';
import RemoveButton from "./remove-button";

export default class String extends React.Component {

    static handleValidation(value) {
        const length = value.length;
        if (length === 0) return null;
        return 'success';
    }

    render() {
        return (
            <FormGroup
                controlId="formBasicText"
                validationState={String.handleValidation(this.props.value)}
            >
                <div className="input-group mb-3">
                    <FormControl
                        type="text"
                        value={this.props.value}
                        onChange={(e) => this.props.onChange(e.target.value)}
                    />
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